package org.tastefuljava.tomcat;

import java.util.ArrayList;
import java.util.List;

public class QueryParser {
    private final StringBuilder buf = new StringBuilder();
    private final List<String> names = new ArrayList<String>();

    public String getQuery() {
        return buf.toString();
    }

    public String[] getNames() {
        return names.toArray(new String[names.size()]);
    }

    public void parse(String query) {
        char[] chars = query.toCharArray();
        int st = 0;
        int mark = -1;
        for (char c: chars) {
            switch (st) {
                case 0:
                    if (c == ':') {
                        st = 1;
                        mark = buf.length();
                    } else {
                        buf.append(c);
                        if (c == '\'') {
                            st = 2;
                        } else if (c == '"') {
                            st = 3;
                        }
                    }
                    break;
                case 1:
                    if (!Character.isLetter(c) && !Character.isDigit(c)
                            && c != '_') {
                        saveName(mark);
                        st = 0;
                    }
                    buf.append(c);
                    break;
                case 2:
                    buf.append(c);
                    if (c == '\'') {
                        st = 0;
                    }
                    break;
                case 3:
                    buf.append(c);
                    if (c == '"') {
                        st = 0;
                    }
                    break;
            }
        }
        if (st == 1) {
            saveName(mark);
        }
    }

    private void saveName(int mark) {
        names.add(buf.substring(mark));
        buf.setLength(mark);
        buf.append('?');
    }
}
