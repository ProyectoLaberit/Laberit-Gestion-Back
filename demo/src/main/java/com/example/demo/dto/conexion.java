package com.example.demo.dto;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class conexion {
        private static final String URL = "jdbc:postgresql://ep-sparkling-dawn-ageup1rq-pooler.c-2.eu-central-1.aws.neon.tech/neondb?&sslmode=require&channelBinding=require";
        private static final String USER = "neondb_owner";
        private static final String PASSWORD = "npg_vJy6uCUrxaH9";

        public static Connection getConnection() throws Exception {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        }

        public static String[][] cargarProyectos() {

            String query = "SELECT id_proyecto,nombre,descripcion FROM proyecto";
            List<String[]> dataList = new ArrayList<>();
            dataList.add(new String[]{"id", "nombre", "descripcion"});

            try (Connection con = getConnection();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(query)) {

                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    String[] row = new String[columnCount];
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = rs.getObject(i);
                        row[i - 1] = value != null ? value.toString() : "";
                    }
                    dataList.add(row);
                }

                String[][] datos = new String[dataList.size()][];
                return dataList.toArray(datos);

            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

        }
}
