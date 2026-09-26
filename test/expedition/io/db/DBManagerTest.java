package expedition.io.db;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.Test;
import static org.junit.Assert.*;

public class DBManagerTest {
    @Test
    public void getReturnsTheSameConnectionWhenCalledTwice() throws SQLException {
        Connection first = DBManager.get();
        Connection second = DBManager.get();
        assertSame("get() should cache and reuse one connection", first, second);
    }
}
