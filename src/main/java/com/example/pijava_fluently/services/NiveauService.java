package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Niveau;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NiveauService implements IService<Niveau> {

    private Connection cnx = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(Niveau niveau) throws SQLException {
        String sql = "INSERT INTO niveau (titre, description, image_couverture, difficulte, ordre, seuil_score_max, seuil_score_min, id_langue_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, niveau.getTitre());
        ps.setString(2, niveau.getDescription());
        ps.setString(3, niveau.getImageCouverture());
        ps.setString(4, niveau.getDifficulte());
        ps.setInt(5, niveau.getOrdre());
        ps.setDouble(6, niveau.getSeuilScoreMax());
        ps.setDouble(7, niveau.getSeuilScoreMin());
        ps.setInt(8, niveau.getIdLangueId());
        ps.executeUpdate();
        System.out.println("Niveau ajouté avec succès !");
    }

    @Override
    public void modifier(Niveau niveau) throws SQLException {
        String sql = "UPDATE niveau SET titre=?, description=?, image_couverture=?, difficulte=?, ordre=?, seuil_score_max=?, seuil_score_min=?, id_langue_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, niveau.getTitre());
        ps.setString(2, niveau.getDescription());
        ps.setString(3, niveau.getImageCouverture());
        ps.setString(4, niveau.getDifficulte());
        ps.setInt(5, niveau.getOrdre());
        ps.setDouble(6, niveau.getSeuilScoreMax());
        ps.setDouble(7, niveau.getSeuilScoreMin());
        ps.setInt(8, niveau.getIdLangueId());
        ps.setInt(9, niveau.getId());
        ps.executeUpdate();
        System.out.println("Niveau modifié avec succès !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM niveau WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Niveau supprimé avec succès !");
    }

    @Override
    public List<Niveau> recuperer() throws SQLException {
        List<Niveau> niveaux = new ArrayList<>();
        String sql = "SELECT * FROM niveau";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Niveau n = new Niveau();
            n.setId(rs.getInt("id"));
            n.setTitre(rs.getString("titre"));
            n.setDescription(rs.getString("description"));
            n.setImageCouverture(rs.getString("image_couverture"));
            n.setDifficulte(rs.getString("difficulte"));
            n.setOrdre(rs.getInt("ordre"));
            n.setSeuilScoreMax(rs.getDouble("seuil_score_max"));
            n.setSeuilScoreMin(rs.getDouble("seuil_score_min"));
            n.setIdLangueId(rs.getInt("id_langue_id"));
            niveaux.add(n);
        }
        return niveaux;
    }
}