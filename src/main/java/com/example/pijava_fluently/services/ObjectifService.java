package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Objectif;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ObjectifService {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    public void ajouter(Objectif o) throws SQLException {
        String sql = "INSERT INTO objectif (titre, description, date_deb, date_fin, statut, id_user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, o.getTitre());
        ps.setString(2, o.getDescription());
        ps.setDate(3, Date.valueOf(o.getDateDeb()));
        ps.setDate(4, Date.valueOf(o.getDateFin()));
        ps.setString(5, o.getStatut());
        ps.setInt(6, o.getIdUserId());
        ps.executeUpdate();
        System.out.println("Objectif ajouté avec succès !");
    }

    public void modifier(Objectif o) throws SQLException {
        String sql = "UPDATE objectif SET titre=?, description=?, date_deb=?, date_fin=?, statut=?, id_user_id=? " +
                "WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, o.getTitre());
        ps.setString(2, o.getDescription());
        ps.setDate(3, Date.valueOf(o.getDateDeb()));
        ps.setDate(4, Date.valueOf(o.getDateFin()));
        ps.setString(5, o.getStatut());
        ps.setInt(6, o.getIdUserId());
        ps.setInt(7, o.getId());
        ps.executeUpdate();
        System.out.println("Objectif modifié avec succès !");
    }

    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM objectif WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Objectif supprimé avec succès !");
    }

    public List<Objectif> recuperer() throws SQLException {
        List<Objectif> list = new ArrayList<>();
        String sql = "SELECT * FROM objectif";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Objectif o = new Objectif();
            o.setId(rs.getInt("id"));
            o.setTitre(rs.getString("titre"));
            o.setDescription(rs.getString("description"));
            o.setDateDeb(rs.getDate("date_deb").toLocalDate());
            o.setDateFin(rs.getDate("date_fin").toLocalDate());
            o.setStatut(rs.getString("statut"));
            o.setIdUserId(rs.getInt("id_user_id"));
            list.add(o);
        }
        return list;
    }
}