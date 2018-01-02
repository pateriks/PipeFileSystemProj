/*package company.server;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class SQLar {
    public static void main(String [] s){
        final String url = "jdbc:mysql://localhost/data";
        final String driver = "mysql-connector-java-5.0.8-bin.jar";
        Driver d;
        d = new RuntimeDriverLoader().loadDriver("mysql-connector-java-5.0.8-bin.jar");
        int size = 1;
        for(int i=0;i<size;i++) {
            Connection con = null;
            try {
                DriverManager.registerDriver(new DriverWrapper(d));
                con = DriverManager.getConnection(url, "root", "mypass123");
                Statement st = con.createStatement();
                int bool = st.executeUpdate("INSERT INTO users " + "VALUES ('Patrik', 'Eriksson', 1)");
                System.out.println("he");
            } catch (SQLException e) {
                int a = e.getErrorCode();
                if (a == 0) {System.out.println("Fatal error: Database connection failed"); }
                else if (a == 1045){System.out.println("Fatal error("+i+"): Access denied"); }
                else if (a == 1044){System.out.println("Fatal error("+i+"): Access denied"); }
                else if (a == 1049){System.out.println("Fatal error: Database doesnt exist"); }
                else {System.out.println("Fatal error: " + e.toString());}
            }
        }
    }
}
class DriverWrapper implements Driver {
    private Driver driver;
    public DriverWrapper(Driver d) {
        this.driver = d;
    }
    public boolean acceptsURL(String url) throws SQLException {
        return this.driver.acceptsURL(url);
    }
    public Connection connect(String url, Properties info) throws SQLException {
        return this.driver.connect(url, info);
    }
    public int getMajorVersion() {
        return this.driver.getMajorVersion();
    }
    public int getMinorVersion() {
        return this.driver.getMinorVersion();
    }
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return this.driver.getPropertyInfo(url, info);
    }
    public boolean jdbcCompliant() {
        return this.driver.jdbcCompliant();
    }
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }
}
class RuntimeDriverLoader {
    public Driver loadDriver(String path, boolean trace) {
        URL u = null;
        Driver d = null;
        try {
            u = new URL("jar:file:" + path + "!/");
        } catch (MalformedURLException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        String classname = "com.mysql.jdbc.Driver";
        URLClassLoader ucl = new URLClassLoader(new URL[] { u });

        try {
            d = (Driver)Class.forName(classname, true, ucl).newInstance();
        } catch (InstantiationException e1) {
            // TODO Auto-generated catch block
            if (trace) e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            // TODO Auto-generated catch block
            if (trace) e1.printStackTrace();
        } catch (ClassNotFoundException e1) {
            // TODO Auto-generated catch block
            if (trace) e1.printStackTrace();
            return null;
        }
        return d;
    }
    public Driver loadDriver(String path){
        return this.loadDriver(path, true);
    }
}
*/
