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
    }

    public void modifier(Objectif o) throws SQLException {
        String sql = "UPDATE objectif SET titre=?, description=?, date_deb=?, date_fin=?, statut=?, id_user_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, o.getTitre());
        ps.setString(2, o.getDescription());
        ps.setDate(3, Date.valueOf(o.getDateDeb()));
        ps.setDate(4, Date.valueOf(o.getDateFin()));
        ps.setString(5, o.getStatut());
        ps.setInt(6, o.getIdUserId());
        ps.setInt(7, o.getId());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("DELETE FROM objectif WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<Objectif> recuperer() throws SQLException {
        List<Objectif> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM objectif");
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

    public List<Objectif> recupererParUtilisateur(int userId) throws SQLException {
        List<Objectif> list = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement("SELECT * FROM objectif WHERE id_user_id = ?");
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
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

    public Objectif recupererParId(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("SELECT * FROM objectif WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return new Objectif(
                    rs.getInt("id"),
                    rs.getString("titre"),
                    rs.getString("description"),
                    rs.getDate("date_deb").toLocalDate(),
                    rs.getDate("date_fin").toLocalDate(),
                    rs.getString("statut"),
                    rs.getInt("id_user_id")
            );
        }
        return null;
    }
}