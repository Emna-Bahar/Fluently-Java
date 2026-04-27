package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.utils.MyDatabase;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {

    private final Connection connection;

    public UserService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public void ajouter(User u) throws SQLException {
        String sql = "INSERT INTO user "
                + "(email, nom, prenom, statut, password, roles, face_descriptor, chosen_language, avatar_svg) "
                + "VALUES (?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getEmail());
            ps.setString(2, u.getNom());
            ps.setString(3, u.getPrenom());
            ps.setString(4, u.getStatut() != null ? u.getStatut() : "actif");
            ps.setString(5, hashPassword(u.getPassword()));
            ps.setString(6, u.getRoles() != null ? u.getRoles() : "[\"ROLE_ETUDIANT\"]");
            ps.setString(7, u.getFaceDescriptor());
            ps.setString(8, u.getChosenLanguage());   // NEW
            ps.setString(9, u.getAvatarSvg());        // NEW
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) u.setId(keys.getInt(1));
            }
        }
    }

    // ── READ ──────────────────────────────────────────────────────────────────

    @Override
    public List<User> recuperer() throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, email, nom, prenom, statut, password, roles, "
                + "face_descriptor, chosen_language, avatar_svg FROM user";
        try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT id, email, nom, prenom, statut, password, roles, "
                + "face_descriptor, chosen_language, avatar_svg FROM user WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapRow(rs); }
        }
        return null;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT id, email, nom, prenom, statut, password, roles, "
                + "face_descriptor, chosen_language, avatar_svg FROM user WHERE email=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return mapRow(rs); }
        }
        return null;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Override
    public void modifier(User u) throws SQLException {
        String sql = "UPDATE user SET email=?, nom=?, prenom=?, statut=?, password=?, roles=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, u.getEmail());
            ps.setString(2, u.getNom());
            ps.setString(3, u.getPrenom());
            ps.setString(4, u.getStatut());
            ps.setString(5, isHashed(u.getPassword()) ? u.getPassword() : hashPassword(u.getPassword()));
            ps.setString(6, u.getRoles());
            ps.setInt(7, u.getId());
            ps.executeUpdate();
        }
    }

    /** Saves just the AI-generated SVG avatar for a user. */
    public void updateAvatar(int userId, String avatarSvg) throws SQLException {
        String sql = "UPDATE user SET avatar_svg = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, avatarSvg);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    /** Saves the language the user chose to study. */
    public void updateChosenLanguage(int userId, String language) throws SQLException {
        String sql = "UPDATE user SET chosen_language = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, language);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Override
    public void supprimer(int id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM user WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    // ── SEARCH ───────────────────────────────────────────────────────────────

    public List<User> search(String keyword) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT id, email, nom, prenom, statut, password, roles, "
                + "face_descriptor, chosen_language, avatar_svg "
                + "FROM user WHERE nom LIKE ? OR prenom LIKE ? OR email LIKE ?";
        String like = "%" + keyword + "%";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, like); ps.setString(2, like); ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapRow(rs)); }
        }
        return list;
    }

    // ── AUTH ─────────────────────────────────────────────────────────────────

    public User authenticate(String email, String password) throws SQLException {
        User user = findByEmail(email);
        if (user == null) return null;
        if (!BCrypt.checkpw(password, user.getPassword())) return null;
        return user;
    }

    public void updateStatut(int id, String statut) throws SQLException {
        String sql = "UPDATE user SET statut=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public int count() throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM user")) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    // ── ROW MAPPER ────────────────────────────────────────────────────────────

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setEmail(rs.getString("email"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setStatut(rs.getString("statut"));
        u.setPassword(rs.getString("password"));
        u.setRoles(rs.getString("roles"));
        u.setFaceDescriptor(rs.getString("face_descriptor"));
        u.setChosenLanguage(rs.getString("chosen_language"));   // NEW
        u.setAvatarSvg(rs.getString("avatar_svg"));             // NEW
        return u;
    }

    // ── BCRYPT HELPERS ────────────────────────────────────────────────────────

    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
    }

    public static boolean isHashed(String password) {
        return password != null && password.startsWith("$2");
    }

    // ── MIGRATION: rehash plain-text passwords ────────────────────────────────

    public void migratePasswordsIfNeeded() {
        try {
            for (User u : recuperer()) {
                if (!isHashed(u.getPassword())) {
                    String hashed = hashPassword(u.getPassword());
                    String sql = "UPDATE user SET password=? WHERE id=?";
                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        ps.setString(1, hashed);
                        ps.setInt(2, u.getId());
                        ps.executeUpdate();
                    }
                    System.out.println("🔐 Password migrated for: " + u.getEmail());
                }
            }
        } catch (SQLException e) {
            System.err.println("Migration error: " + e.getMessage());
        }
    }
}