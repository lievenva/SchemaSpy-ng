package net.sourceforge.schemaspy.util;

public class DbSpecificOption {
    private final String name;
    private       Object value;
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
    
    public Object getValue() {
        return value;
    }
    
    public void setValue(Object value) {
        this.value = value;
    }
    
    public String getDescription() {
        return description;
    }
    
    @Override
    public String toString() {
        return description;
    }
}

