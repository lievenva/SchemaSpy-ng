package net.sourceforge.schemaspy.util;

public class DbSpecificOption {
    private final String name;
    private       String value;
    private final String description;
    
    public DbSpecificOption(String name, String value, String description) {
        this.name = name;
        this.value = value;
        this.description = description;
    }
    
    public DbSpecificOption(String name, String description) {
        this(name, null, description);
    }
    
    public String getName() {
        return name;
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String toString() {
        return description;
    }
}

