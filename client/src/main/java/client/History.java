package client;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class History {

    private PrintWriter out;

    private String getHistoryByLogin(String login) {
        return "history/history_" + login + ".txt";
    }

    public void start(String login) {
        try {
            out = new PrintWriter(new FileOutputStream(getHistoryByLogin(login), true), true);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (out != null) {
            out.close();
        }
    }

    public void writeLine(String msg) {
        out.println(msg);
    }

    public String getLastLines(String login) {
        if (!Files.exists(Paths.get(getHistoryByLogin(login)))) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try {
            List<String> historyLines = Files.readAllLines(Paths.get(getHistoryByLogin(login)));
            int start = 0;
            if (historyLines.size() > 100) {
                start = historyLines.size() - 100;
            }
            for (int i = start; i < historyLines.size(); i++) {
                sb.append(historyLines.get(i)).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
