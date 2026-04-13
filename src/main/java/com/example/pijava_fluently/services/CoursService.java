package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Cours;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoursService implements IService<Cours> {

    private Connection cnx = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(Cours cours) throws SQLException {
        String sql = "INSERT INTO cours (numero, ressource, date_creation, cours_precedent_id_id, id_niveau_id) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, cours.getNumero());
        ps.setString(2, cours.getRessource());
        ps.setDate(3, Date.valueOf(cours.getDateCreation()));
        // cours_precedent peut être null (premier cours du niveau)
        if (cours.getCoursPrecedentIdId() == 0) {
            ps.setNull(4, Types.INTEGER);
        } else {
            ps.setInt(4, cours.getCoursPrecedentIdId());
        }
        ps.setInt(5, cours.getIdNiveauId());
        ps.executeUpdate();
        System.out.println("Cours ajouté avec succès !");
    }

    @Override
    public void modifier(Cours cours) throws SQLException {
        String sql = "UPDATE cours SET numero=?, ressource=?, date_creation=?, cours_precedent_id_id=?, id_niveau_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, cours.getNumero());
        ps.setString(2, cours.getRessource());
        ps.setDate(3, Date.valueOf(cours.getDateCreation()));
        if (cours.getCoursPrecedentIdId() == 0) {
            ps.setNull(4, Types.INTEGER);
        } else {
            ps.setInt(4, cours.getCoursPrecedentIdId());
        }
        ps.setInt(5, cours.getIdNiveauId());
        ps.setInt(6, cours.getId());
        ps.executeUpdate();
        System.out.println("Cours modifié avec succès !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM cours WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Cours supprimé avec succès !");
    }

    @Override
    public List<Cours> recuperer() throws SQLException {
        List<Cours> coursList = new ArrayList<>();
        String sql = "SELECT * FROM cours";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Cours c = new Cours();
            c.setId(rs.getInt("id"));
            c.setNumero(rs.getInt("numero"));
            c.setRessource(rs.getString("ressource"));
            c.setDateCreation(rs.getDate("date_creation").toLocalDate());
            c.setCoursPrecedentIdId(rs.getInt("cours_precedent_id_id")); // retourne 0 si NULL
            c.setIdNiveauId(rs.getInt("id_niveau_id"));
            coursList.add(c);
        }
        return coursList;
    }
}