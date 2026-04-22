package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Tache;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TacheService {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    public void ajouter(Tache t) throws SQLException {
        String sql = "INSERT INTO tache (titre, description, date_limite, statut, priorite, id_objectif_id) VALUES (?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, t.getTitre());
        ps.setString(2, t.getDescription());
        ps.setDate(3, Date.valueOf(t.getDateLimite()));
        ps.setString(4, t.getStatut());
        ps.setString(5, t.getPriorite());
        ps.setInt(6, t.getIdObjectifId());
        ps.executeUpdate();
    }

    public void modifier(Tache t) throws SQLException {
        String sql = "UPDATE tache SET titre=?, description=?, date_limite=?, statut=?, priorite=?, id_objectif_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, t.getTitre());
        ps.setString(2, t.getDescription());
        ps.setDate(3, Date.valueOf(t.getDateLimite()));
        ps.setString(4, t.getStatut());
        ps.setString(5, t.getPriorite());
        ps.setInt(6, t.getIdObjectifId());
        ps.setInt(7, t.getId());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("DELETE FROM tache WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<Tache> recuperer() throws SQLException {
        List<Tache> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM tache");
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    public List<Tache> recupererParObjectif(int idObjectif) throws SQLException {
        List<Tache> list = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement("SELECT * FROM tache WHERE id_objectif_id = ?");
        ps.setInt(1, idObjectif);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    public Tache recupererParId(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("SELECT * FROM tache WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return map(rs);
        }
        return null;
    }

    private Tache map(ResultSet rs) throws SQLException {
        Tache t = new Tache();
        t.setId(rs.getInt("id"));
        t.setTitre(rs.getString("titre"));
        t.setDescription(rs.getString("description"));
        Date dateLimite = rs.getDate("date_limite");
        if (dateLimite != null) {
            t.setDateLimite(dateLimite.toLocalDate());
        }
        t.setStatut(rs.getString("statut"));
        t.setPriorite(rs.getString("priorite"));
        t.setIdObjectifId(rs.getInt("id_objectif_id"));
        return t;
    }
}