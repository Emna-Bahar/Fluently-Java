package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Reponse;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReponseService implements IService<Reponse> {
    private Connection cnx = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(Reponse r) throws SQLException {
        String sql = "INSERT INTO reponse (contenu_rep, is_correct, score, date_reponse, id_question_id) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, r.getContenuRep());
        ps.setBoolean(2, r.isCorrect());
        ps.setInt(3, r.getScore());
        ps.setDate(4, r.getDateReponse() != null ? Date.valueOf(r.getDateReponse()) : null);
        ps.setInt(5, r.getQuestionId());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Reponse r) throws SQLException {
        String sql = "UPDATE reponse SET contenu_rep=?, is_correct=?, score=?, date_reponse=?, id_question_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, r.getContenuRep());
        ps.setBoolean(2, r.isCorrect());
        ps.setInt(3, r.getScore());
        ps.setDate(4, r.getDateReponse() != null ? Date.valueOf(r.getDateReponse()) : null);
        ps.setInt(5, r.getQuestionId());
        ps.setInt(6, r.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM reponse WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Reponse> recuperer() throws SQLException {
        List<Reponse> list = new ArrayList<>();
        String sql = "SELECT * FROM reponse";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Reponse r = new Reponse();
            r.setId(rs.getInt("id"));
            r.setContenuRep(rs.getString("contenu_rep"));
            r.setCorrect(rs.getBoolean("is_correct"));
            r.setScore(rs.getInt("score"));
            Date d = rs.getDate("date_reponse");
            if (d != null) r.setDateReponse(d.toLocalDate());
            r.setQuestionId(rs.getInt("id_question_id"));
            list.add(r);
        }
        return list;
    }

    public List<Reponse> recupererParQuestion(int questionId) throws SQLException {
        List<Reponse> list = new ArrayList<>();
        String sql = "SELECT * FROM reponse WHERE id_question_id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, questionId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Reponse r = new Reponse();
            r.setId(rs.getInt("id"));
            r.setContenuRep(rs.getString("contenu_rep"));
            r.setCorrect(rs.getBoolean("is_correct"));
            r.setScore(rs.getInt("score"));
            Date d = rs.getDate("date_reponse");
            if (d != null) r.setDateReponse(d.toLocalDate());
            r.setQuestionId(rs.getInt("id_question_id"));
            list.add(r);
        }
        return list;
    }
}