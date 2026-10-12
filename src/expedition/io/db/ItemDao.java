/*
 * ItemDao
 * Reads and writes the ITEMS table. The only class that knows the SQL for items,
 * so the rest of the program just deals with ItemDefinition objects
*/
package expedition.io.db;

import expedition.exception.DatabaseException;
import expedition.item.ItemDefinition;
import expedition.item.ItemKind;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

class ItemDao {

    // ORDER BY ID so they come back in the same order they were in items.txt
    private static final String SELECT_ALL = "SELECT NAME, KIND, AMOUNT, BONUS, DROP_WEIGHT, DESCRIPTION FROM ITEMS ORDER BY ID";

    // ID is left out, the database numbers the rows itself
    private static final String INSERT = "INSERT INTO ITEMS (NAME, KIND, AMOUNT, BONUS, DROP_WEIGHT, DESCRIPTION) VALUES (?, ?, ?, ?, ?, ?)";

    void insertAll(List<ItemDefinition> items) throws DatabaseException {
        try (PreparedStatement ps = DBManager.get().prepareStatement(INSERT)) {
            for (ItemDefinition d : items) {
                ps.setString(1, d.name());
                ps.setString(2, d.kind().name());
                ps.setInt(3, d.amount());
                ps.setInt(4, d.bonus());
                ps.setInt(5, d.dropWeight());
                ps.setString(6, d.description());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new DatabaseException("Could not save the items", e);
        }
    }

    List<ItemDefinition> findAll() throws DatabaseException {
        List<ItemDefinition> items = new ArrayList<>();

        try (Statement st = DBManager.get().createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL)) {

            while (rs.next()) {
                // KIND is stored as text, valueOf turns "MEDKIT" back into ItemKind.MEDKIT
                items.add(new ItemDefinition(
                        rs.getString("NAME"),
                        ItemKind.valueOf(rs.getString("KIND")),
                        rs.getInt("AMOUNT"),
                        rs.getInt("BONUS"),
                        rs.getInt("DROP_WEIGHT"),
                        rs.getString("DESCRIPTION")));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Could not read the items", e);
        }
        return items;
    }
}
