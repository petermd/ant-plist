package petermd.ant.plist;

import com.dd.plist.*;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.util.*;

/** ANT Task for editing Property-List files */
public class PlistTask extends Task {

    // Definitions

    /** Empty Dictionary */
    public final Dictionary EMPTY_DICTIONARY=new Dictionary();

    /** Value */
    public class Value
    {
        /** Type */
        protected String valueType;

        /** Value */
        protected String value;

        /** Create new Node */
        public Value(String valueType, String defaultValue) {
            this.valueType=valueType;
            this.value=defaultValue;
        }

        /** Return string representation */
        public String toString() {
            return "<"+valueType+">"+value+"</"+valueType+">";
        }

        /** Set text */
        public void addText(String value) {
            this.value=getProject().replaceProperties(value);
        }

        /** Return NS value */
        public Object toNSValue() throws Exception
        {
            switch(valueType)
            {
                case "boolean":
                    return Boolean.valueOf(this.value);
                case "integer":
                    return Integer.valueOf(this.value);
                case "real":
                    return Double.valueOf(this.value);
                case "data":
                    return new NSData(this.value);
                case "date":
                    return new NSDate(this.value);
                default:
                    return this.value;
            }
        }
    }

    /** Set */
    public abstract class Set extends Value {

        /** Set */
        public Set(String setType)
        {
            super(setType,"");
        }

        /** String */
        public Value createString() {
            return add(new Value("string", ""));
        }

        /** Data */
        public Value createData() {
            return add(new Value("data", ""));
        }

        /** Date */
        public Value createDate() {
            return add(new Value("date", ""));
        }

        /** Integer */
        public Value createInteger() {
            return add(new Value("integer", "0"));
        }

        /** Real */
        public Value createReal() {
            return add(new Value("real", "0.0"));
        }

        /** Boolean */
        public Value createBoolean() {
            return add(new Value("boolean", "false"));
        }

        // Implementation

        /** Add value */
        protected abstract Value add(Value v);
    }

    /** Array */
    public class Array extends Set {

        /** Data */
        protected final List<Value> data;

        /** Create new Array */
        public Array()
        {
            super("array");

            this.data=new ArrayList<Value>();
        }

        // Implementation

        /** Add */
        @Override protected Value add(Value v) {
            this.data.add(v);
            return v;
        }
    }

    /** Dictionary */
    public class Dictionary extends Set {

        /** Data */
        protected Map<String,Value> data;

        /** Last key */
        protected Value nextKey;

        /** Create new Dictionary */
        public Dictionary() {
            super("dict");

            this.data=new HashMap<String,Value>();
            this.nextKey=null;
        }

        /** Add key */
        public Value createKey() {
            this.nextKey=new Value("key","");
            return this.nextKey;
        }

        /** Nested array */
        public Array createArray() {
            return (Array)add(new Array());
        }

        /** Nested dictionary */
        public Dictionary createDict() {
            return (Dictionary)add(new Dictionary());
        }

        // Implementation

        /** Add key */
        @Override protected Value add(Value v) {

            if (this.nextKey==null) {
                throw new IllegalArgumentException("Value without key");
            }

            this.data.put(this.nextKey.value,v);

            this.nextKey=null;

            return v;
        }
    }

    // Instance variables

    /** File */
    protected File file;

    /** Format */
    protected String format="xml";

    /** Property prefix */
    protected String propertyPrefix;

    /** Data */
    protected Dictionary dict;

    // Public methods

    /** Create new PList task */
    public PlistTask() {
        this.dict=EMPTY_DICTIONARY;
    }

    /** Dictionary */
    public Dictionary createDict() {
        Dictionary d=new Dictionary();
        this.dict=d;
        return d;
    }

    /** Format to use */
    public void setFormat(String value) {
        this.format=value;
    }

    /** File to edit */
    public void setFile(File value) {
        this.file=value;
    }

    /** Set property to output */
    public void setPropertyPrefix(String value) {
        this.propertyPrefix=value;
    }

    // Task implememtation

    /** Do the work. */
    @Override public void execute()
    {
        // Validate file
        if (this.file==null || !this.file.exists() || !this.file.canRead())
            throw new BuildException("Invalid 'file' specified");

        log("Loading plist file '" + this.file.getAbsolutePath() + "'", Project.MSG_VERBOSE);

        try
        {
            NSDictionary root=(NSDictionary)PropertyListParser.parse(file);

            for (Map.Entry<String,Value> p : this.dict.data.entrySet())
            {
                Object origValue=root.get(p.getKey());
                Value newValue=p.getValue();

                switch (newValue.valueType)
                {
                    case "array":
                        root.put(p.getKey(),mapArray((Array)newValue));
                        break;
                    case "dict":
                        root.put(p.getKey(),mapDict((Dictionary)newValue));
                        break;
                    default:
                        root.put(p.getKey(),newValue.toNSValue());
                        break;
                }

                if (origValue==null) {
                    log("Added new property '"+p.getKey()+"'='"+p.getValue()+"'", Project.MSG_DEBUG);
                }
                else {
                    log("Replaced property '"+p.getKey()+"'='"+origValue+"' with '"+p.getValue()+"'", Project.MSG_DEBUG);
                }
            }

            if (!this.dict.data.isEmpty())
            {
                log("Re-writing '" + this.file.getAbsolutePath() + "' (format="+this.format+")",Project.MSG_VERBOSE);

                if ("binary".equals(this.format))
                {
                    PropertyListParser.saveAsBinary(root, this.file);
                }
                else
                {
                    PropertyListParser.saveAsXML(root, this.file);
                }
            }

            if (this.propertyPrefix!=null)
            {
                log("Exporting plist as properties (prefix="+this.propertyPrefix+")",Project.MSG_VERBOSE);
                for (String k : root.allKeys())
                {
                    log("Export property "+this.propertyPrefix+"."+k+"="+root.get(k),Project.MSG_DEBUG);
                    this.getProject().setProperty(this.propertyPrefix+"."+k,root.get(k).toString());
                }
            }
        }
        catch(Throwable t)
        {
            throw new BuildException("Unable to read plist file",t);
        }
    }

    // Implementation

    /** Convert Array to NSArray */
    protected NSArray mapArray(Array src) throws Exception
    {
        NSArray ns=new NSArray(src.data.size());
        for (int i=0;i<src.data.size();i++)
        {
            ns.setValue(i,src.data.get(i).toNSValue());
        }
        return ns;
    }

    /** Convert Dictionary to NSArray */
    protected NSDictionary mapDict(Dictionary src) throws Exception
    {
        NSDictionary ns=new NSDictionary();
        for (Map.Entry<String,Value> p : src.data.entrySet())
        {
            Value newValue=p.getValue();

            switch (newValue.valueType)
            {
                case "array":
                    ns.put(p.getKey(),mapArray((Array)newValue));
                    break;
                case "dict":
                    ns.put(p.getKey(),mapDict((Dictionary)newValue));
                    break;
                default:
                    ns.put(p.getKey(),newValue.toNSValue());
                    break;
            }
        }
        return ns;
    }
}

