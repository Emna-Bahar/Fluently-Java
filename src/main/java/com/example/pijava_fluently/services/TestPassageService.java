package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.TestPassage;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TestPassageService implements IService<TestPassage> {
    private Connection cnx = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(TestPassage tp) throws SQLException {
        String sql = "INSERT INTO test_passage (date_debut, date_fin, resultat, score, score_max, statut, temps_passe, test_id, user_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setTimestamp(1, tp.getDateDebut() != null ? Timestamp.valueOf(tp.getDateDebut()) : null);
        ps.setTimestamp(2, tp.getDateFin()   != null ? Timestamp.valueOf(tp.getDateFin())   : null);
        ps.setDouble(3, tp.getResultat());
        ps.setInt(4, tp.getScore());
        ps.setInt(5, tp.getScoreMax());
        ps.setString(6, tp.getStatut());
        ps.setInt(7, tp.getTempsPasse());
        ps.setInt(8, tp.getTestId());
        ps.setInt(9, tp.getUserId());
        ps.executeUpdate();
    }

    @Override
    public void modifier(TestPassage tp) throws SQLException {
        String sql = "UPDATE test_passage SET date_debut=?, date_fin=?, resultat=?, score=?, score_max=?, statut=?, temps_passe=?, test_id=?, user_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setTimestamp(1, tp.getDateDebut() != null ? Timestamp.valueOf(tp.getDateDebut()) : null);
        ps.setTimestamp(2, tp.getDateFin()   != null ? Timestamp.valueOf(tp.getDateFin())   : null);
        ps.setDouble(3, tp.getResultat());
        ps.setInt(4, tp.getScore());
        ps.setInt(5, tp.getScoreMax());
        ps.setString(6, tp.getStatut());
        ps.setInt(7, tp.getTempsPasse());
        ps.setInt(8, tp.getTestId());
        ps.setInt(9, tp.getUserId());
        ps.setInt(10, tp.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM test_passage WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<TestPassage> recuperer() throws SQLException {
        List<TestPassage> list = new ArrayList<>();
        String sql = "SELECT * FROM test_passage";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            TestPassage tp = new TestPassage();
            tp.setId(rs.getInt("id"));
            Timestamp dd = rs.getTimestamp("date_debut");
            if (dd != null) tp.setDateDebut(dd.toLocalDateTime());
            Timestamp df = rs.getTimestamp("date_fin");
            if (df != null) tp.setDateFin(df.toLocalDateTime());
            tp.setResultat(rs.getDouble("resultat"));
            tp.setScore(rs.getInt("score"));
            tp.setScoreMax(rs.getInt("score_max"));
            tp.setStatut(rs.getString("statut"));
            tp.setTempsPasse(rs.getInt("temps_passe"));
            tp.setTestId(rs.getInt("test_id"));
            tp.setUserId(rs.getInt("user_id"));
            list.add(tp);
        }
        return list;
    }
}