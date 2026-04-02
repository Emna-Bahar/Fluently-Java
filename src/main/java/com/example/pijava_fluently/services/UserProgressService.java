package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.User_progress;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserProgressService implements IService<User_progress> {

    private Connection cnx = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(User_progress up) throws SQLException {
        String sql = "INSERT INTO user_progress (dernier_numero_cours, test_niveau_complete, date_derniere_activite, user_id, langue_id, niveau_actuel_id, dernier_cours_complete_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, up.getDernierNumeroCours());
        ps.setBoolean(2, up.isTestNiveauComplete());
        ps.setTimestamp(3, Timestamp.valueOf(up.getDateDerniereActivite()));
        ps.setInt(4, up.getUserId());
        ps.setInt(5, up.getLangueId());
        ps.setInt(6, up.getNiveauActuelId());
        ps.setInt(7, up.getDernierCoursCompleteId());
        ps.executeUpdate();
        System.out.println("User progress ajouté avec succès !");
    }

    @Override
    public void modifier(User_progress up) throws SQLException {
        String sql = "UPDATE user_progress SET dernier_numero_cours=?, test_niveau_complete=?, date_derniere_activite=?, user_id=?, langue_id=?, niveau_actuel_id=?, dernier_cours_complete_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, up.getDernierNumeroCours());
        ps.setBoolean(2, up.isTestNiveauComplete());
        ps.setTimestamp(3, Timestamp.valueOf(up.getDateDerniereActivite()));
        ps.setInt(4, up.getUserId());
        ps.setInt(5, up.getLangueId());
        ps.setInt(6, up.getNiveauActuelId());
        ps.setInt(7, up.getDernierCoursCompleteId());
        ps.setInt(8, up.getId());
        ps.executeUpdate();
        System.out.println("User progress modifié avec succès !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM user_progress WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("User progress supprimé avec succès !");
    }

    @Override
    public List<User_progress> recuperer() throws SQLException {
        List<User_progress> progressList = new ArrayList<>();
        String sql = "SELECT * FROM user_progress";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            User_progress up = new User_progress();
            up.setId(rs.getInt("id"));
            up.setDernierNumeroCours(rs.getInt("dernier_numero_cours"));
            up.setTestNiveauComplete(rs.getBoolean("test_niveau_complete"));
            up.setDateDerniereActivite(rs.getTimestamp("date_derniere_activite").toLocalDateTime());
            up.setUserId(rs.getInt("user_id"));
            up.setLangueId(rs.getInt("langue_id"));
            up.setNiveauActuelId(rs.getInt("niveau_actuel_id"));
            up.setDernierCoursCompleteId(rs.getInt("dernier_cours_complete_id"));
            progressList.add(up);
        }
        return progressList;
    }
}