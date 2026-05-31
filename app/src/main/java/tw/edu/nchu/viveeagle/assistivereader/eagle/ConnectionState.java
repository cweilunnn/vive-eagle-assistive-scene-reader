package tw.edu.nchu.viveeagle.assistivereader.eagle;

public enum ConnectionState {
    DISCONNECTED("未連線"),
    CONNECTING("連線中"),
    CONNECTED("已連線");

    private final String displayName;

    ConnectionState(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

