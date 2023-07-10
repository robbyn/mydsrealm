package org.tastefuljava.tomcat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Query {
    private final String sql;
    private final String[] names;

    public static Query parse(String qry) {
        return new Parser().parse(qry);
    }

    public static String sql(Query qry) {
        return qry == null ? null : qry.sql;
    }

    private Query(String sql, String[] names) {
        this.sql = sql;
        this.names = names;
    }

    public PreparedStatement prepare(Connection cnt, Map<String,Object> parms)
            throws SQLException {
        boolean ok = false;
        PreparedStatement stmt = cnt.prepareStatement(sql);
        try {
            int i = 0;
            for (String name: names) {
                Object value = parms.get(name);
                stmt.setObject(++i, value);
            }
            ok = true;
        } finally {
            if (!ok) {
                stmt.close();
            }
        }
        return stmt;
    }

    private static class Parser {
        private final StringBuilder buf = new StringBuilder();
        private final List<String> names = new ArrayList<>();
        private int mark;

        private Query parse(String query) {
            char[] chars = query.toCharArray();
            int st = 0;
            for (char c: chars) {
                st = transition(st, c);
            }
            if (st == 1) {
                endName();
            }
            return new Query(buf.toString(), names.toArray(new String[0]));
        }

        private int transition(int st, char c) {
            switch (st) {
                case 0:
                    if (c == ':') {
                        startName();
                        return 1;
                    } else {
                        buf.append(c);
                        if (c == '\'') {
                            return 2;
                        } else if (c == '"') {
                            return 3;
                        }
                    }
                    break;
                case 1:
                    if (!Character.isLetter(c) && !Character.isDigit(c)
                            && c != '_') {
                        endName();
                        return transition(0, c);
                    } else {
                        buf.append(c);
                    }
                    break;
                case 2:
                    buf.append(c);
                    if (c == '\'') {
                        return 0;
                    }
                    break;
                case 3:
                    buf.append(c);
                    if (c == '"') {
                        return 0;
                    }
                    break;
            }
            return st;
        }

        private void startName() {
            mark = buf.length();
        }

        private void endName() {
            names.add(buf.substring(mark));
            buf.setLength(mark);
            buf.append('?'); //replace name with '?'
        }
    }
}