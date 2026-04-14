package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TestService implements IService<Test> {
    private Connection cnx = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(Test test) throws SQLException {
        String sql = "INSERT INTO test (type, titre, duree_estimee, langue_id, niveau_id) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, test.getType());
        ps.setString(2, test.getTitre());
        ps.setInt(3, test.getDureeEstimee());
        if (test.getLangueId() == 0) ps.setNull(4, Types.INTEGER);
        else ps.setInt(4, test.getLangueId());
        if (test.getNiveauId() == 0) ps.setNull(5, Types.INTEGER);
        else ps.setInt(5, test.getNiveauId());
        ps.executeUpdate();
    }

    @Override
    public void modifier(Test test) throws SQLException {
        String sql = "UPDATE test SET type=?, titre=?, duree_estimee=?, langue_id=?, niveau_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, test.getType());
        ps.setString(2, test.getTitre());
        ps.setInt(3, test.getDureeEstimee());
        if (test.getLangueId() == 0) ps.setNull(4, Types.INTEGER);
        else ps.setInt(4, test.getLangueId());
        if (test.getNiveauId() == 0) ps.setNull(5, Types.INTEGER);
        else ps.setInt(5, test.getNiveauId());
        ps.setInt(6, test.getId());
        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM test WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public List<Test> recuperer() throws SQLException {
        List<Test> list = new ArrayList<>();
        String sql = "SELECT * FROM test";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Test t = new Test();
            t.setId(rs.getInt("id"));
            t.setType(rs.getString("type"));
            t.setTitre(rs.getString("titre"));
            t.setDureeEstimee(rs.getInt("duree_estimee"));
            t.setLangueId(rs.getInt("langue_id"));
            t.setNiveauId(rs.getInt("niveau_id"));
            list.add(t);
        }
        return list;
    }
}