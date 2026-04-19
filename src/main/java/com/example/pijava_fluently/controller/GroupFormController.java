package com.example.pijava_fluently.controller;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.services.GroupService;
import com.example.pijava_fluently.services.Groupe;
import com.example.pijava_fluently.services.LangueService;
import com.example.pijava_fluently.services.NiveauService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class GroupFormController implements Initializable {

    @FXML private Label formTitle;
    @FXML private TextField txtNom;
    @FXML private TextArea txtDescription;
    @FXML private Spinner<Integer> spinCapacite;
    @FXML private ComboBox<String> comboStatut;
    @FXML private ComboBox<Langue> comboLangue;
    @FXML private ComboBox<Niveau> comboNiveau;
    @FXML private HBox errorBox;
    @FXML private Label errorLabel;
    @FXML private Button btnCancel;
    @FXML private Button btnSave;

    private GroupService groupService;
    private LangueService langueService;
    private NiveauService niveauService;
    private AdminDashboardController parentController;
    private Groupe currentGroupe;
    private boolean isEditMode = false;
    private Runnable onCloseRequest;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        langueService = new LangueService();
        niveauService = new NiveauService();
        
        setupSpinner();
        setupComboBoxes();
        
        // Setup language change listener BEFORE loading languages
        comboLangue.setOnAction(e -> {
            Langue selectedLangue = comboLangue.getValue();
            if (selectedLangue != null) {
                System.out.println("🔍 Language selected: " + selectedLangue.getNom() + " (ID: " + selectedLangue.getId() + ")");
                loadNiveauxForLangue(selectedLangue.getId());
            }
        });
        
        // Load languages after setting up listener
        loadLangues();
    }

    private void setupSpinner() {
        SpinnerValueFactory<Integer> valueFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 30);
        spinCapacite.setValueFactory(valueFactory);
    }

    private void setupComboBoxes() {
        comboStatut.getItems().addAll("actif", "inactif", "complet");
        comboStatut.setValue("actif");
    }

    private void loadLangues() {
        try {
            List<Langue> langues = langueService.recupererToutesLanguesActives();
            comboLangue.getItems().clear();
            comboLangue.getItems().addAll(langues);
            
            System.out.println("📚 Loaded " + langues.size() + " languages");
            for (Langue l : langues) {
                System.out.println("  - " + l.getNom() + " (ID: " + l.getId() + ")");
            }
            
            if (!langues.isEmpty()) {
                comboLangue.setValue(langues.get(0));
                // Manually trigger the level loading for first language
                loadNiveauxForLangue(langues.get(0).getId());
            }
        } catch (SQLException e) {
            showError("Erreur lors du chargement des langues : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadNiveauxForLangue(int idLangue) {
        try {
            System.out.println("🎯 Loading niveaux for langue ID: " + idLangue);
            List<Niveau> niveaux = niveauService.recupererNiveauxParLangue(idLangue);
            comboNiveau.getItems().clear();
            comboNiveau.getItems().addAll(niveaux);
            
            System.out.println("📊 Loaded " + niveaux.size() + " niveaux");
            for (Niveau n : niveaux) {
                System.out.println("  - " + n.getTitre() + " (ID: " + n.getId() + ")");
            }
            
            if (!niveaux.isEmpty()) {
                comboNiveau.setValue(niveaux.get(0));
            } else {
                System.out.println("⚠️ No niveaux found for langue ID: " + idLangue);
            }
        } catch (SQLException e) {
            showError("Erreur lors du chargement des niveaux : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setGroupService(GroupService groupService) {
        this.groupService = groupService;
    }

    public void setParentController(AdminDashboardController parentController) {
        this.parentController = parentController;
    }

    public void setOnCloseRequest(Runnable onCloseRequest) {
        this.onCloseRequest = onCloseRequest;
    }

    public void setGroupe(Groupe groupe) {
        this.currentGroupe = groupe;
        this.isEditMode = true;
        formTitle.setText("Modifier Groupe");
        // Fill form after initialize() has loaded languages
        fillFormWhenReady(groupe);
    }

    private void fillFormWhenReady(Groupe groupe) {
        // Wait for languages to load, then fill form
        comboLangue.getItems().addListener((javafx.collections.ListChangeListener<Langue>) c -> {
            if (!comboLangue.getItems().isEmpty() && currentGroupe != null) {
                fillForm(currentGroupe);
            }
        });
        
        // If already loaded, fill immediately
        if (!comboLangue.getItems().isEmpty()) {
            fillForm(groupe);
        }
    }

    private void fillForm(Groupe groupe) {
        txtNom.setText(groupe.getNom());
        txtDescription.setText(groupe.getDescription());
        spinCapacite.getValueFactory().setValue(groupe.getCapacite());
        comboStatut.setValue(groupe.getStatut());
        
        // Select the correct language
        try {
            Langue langue = langueService.recupererParId(groupe.getIdLangueId());
            if (langue != null) {
                for (Langue l : comboLangue.getItems()) {
                    if (l.getId() == langue.getId()) {
                        comboLangue.setValue(l);
                        break;
                    }
                }
                
                // Load niveaux for this language
                loadNiveauxForLangue(langue.getId());
                
                // Select the correct level after niveaux are loaded
                Niveau niveau = niveauService.recupererParId(groupe.getIdNiveauId());
                if (niveau != null) {
                    for (Niveau n : comboNiveau.getItems()) {
                        if (n.getId() == niveau.getId()) {
                            comboNiveau.setValue(n);
                            break;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            showError("Erreur lors du chargement des données : " + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {
        hideError();
        
        if (!validateForm()) {
            return;
        }

        try {
            Groupe groupe = isEditMode ? currentGroupe : new Groupe();
            groupe.setNom(txtNom.getText().trim());
            groupe.setDescription(txtDescription.getText().trim());
            groupe.setCapacite(spinCapacite.getValue());
            groupe.setStatut(comboStatut.getValue());
            groupe.setIdLangueId(comboLangue.getValue().getId());
            groupe.setIdNiveauId(comboNiveau.getValue().getId());

            if (isEditMode) {
                groupService.modifier(groupe);
            } else {
                groupService.ajouter(groupe);
            }

            if (parentController != null) {
                parentController.loadGroupes();
            }

            closeWindow();

        } catch (SQLException e) {
            showError("Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private boolean validateForm() {
        String nom = txtNom.getText().trim();

        if (nom.isEmpty()) {
            showError("Le nom du groupe est obligatoire");
            txtNom.requestFocus();
            return false;
        }

        if (groupService != null) {
            Integer excludeId = (isEditMode && currentGroupe != null) ? currentGroupe.getId() : null;
            try {
                if (groupService.existsByNom(nom, excludeId)) {
                    showError("Un groupe avec ce nom existe deja");
                    txtNom.requestFocus();
                    return false;
                }
            } catch (SQLException e) {
                showError("Erreur de verification du nom : " + e.getMessage());
                return false;
            }
        }

        if (comboStatut.getValue() == null) {
            showError("Veuillez sélectionner un statut");
            comboStatut.requestFocus();
            return false;
        }

        if (comboLangue.getValue() == null) {
            showError("Veuillez sélectionner une langue");
            comboLangue.requestFocus();
            return false;
        }

        if (comboNiveau.getValue() == null) {
            showError("Veuillez sélectionner un niveau");
            comboNiveau.requestFocus();
            return false;
        }

        return true;
    }

    private void showError(String message) {
        if (errorLabel != null) {
            errorLabel.setText(message);
        }
        if (errorBox != null) {
            errorBox.setVisible(true);
            errorBox.setManaged(true);
        }
    }
    
    private void hideError() {
        if (errorBox != null) {
            errorBox.setVisible(false);
            errorBox.setManaged(false);
        }
    }

    private void closeWindow() {
        if (onCloseRequest != null) {
            onCloseRequest.run();
            return;
        }

        Stage stage = (Stage) btnCancel.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
}
