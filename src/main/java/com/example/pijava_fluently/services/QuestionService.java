package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Question;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionService implements IService<Question> {
    private Connection cnx = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(Question q) throws SQLException {
        String sql = "INSERT INTO question (enonce, type, score_max, id_test_id) VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, q.getEnonce());
        ps.setString(2, q.getType());
        ps.setInt(3, q.getScoreMax());
        ps.setInt(4, q.getTestId());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Question q) throws SQLException {
        String sql = "UPDATE question SET enonce=?, type=?, score_max=?, id_test_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, q.getEnonce());
        ps.setString(2, q.getType());
        ps.setInt(3, q.getScoreMax());
        ps.setInt(4, q.getTestId());
        ps.setInt(5, q.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM question WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Question> recuperer() throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM question";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Question q = new Question();
            q.setId(rs.getInt("id"));
            q.setEnonce(rs.getString("enonce"));
            q.setType(rs.getString("type"));
            q.setScoreMax(rs.getInt("score_max"));
            q.setTestId(rs.getInt("id_test_id"));
            list.add(q);
        }
        return list;
    }

    public List<Question> recupererParTest(int testId) throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT * FROM question WHERE id_test_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, testId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Question q = new Question();
            q.setId(rs.getInt("id"));
            q.setEnonce(rs.getString("enonce"));
            q.setType(rs.getString("type"));
            q.setScoreMax(rs.getInt("score_max"));
            q.setTestId(rs.getInt("id_test_id"));
            list.add(q);
        }
        return list;
    }
}