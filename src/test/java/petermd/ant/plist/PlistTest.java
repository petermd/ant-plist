package petermd.ant.plist;

import org.apache.tools.ant.BuildFileTest;

public class PlistTest extends BuildFileTest {

    public PlistTest(String s) {
        super(s);
    }

    public void setUp() {
        // initialize Ant
        configureProject("build.xml");
    }

    public void testView() {
        executeTarget("use.view");
        assertPropertyEquals("plist.view.first","original-value");
    }

    public void testEdit() {
        executeTarget("use.edit");
        assertPropertyEquals("plist.postedit.first","updated-value");
        assertPropertyEquals("plist.postedit.second","added-value");
    }

    public void testDict() {
        executeTarget("use.dict");
    }

    public void testArray() {
        executeTarget("use.array");
    }

    public void testBinary() {
        executeTarget("use.binary");
        assertPropertyEquals("plist.postbinary.first","updated-binary-value");
        assertPropertyEquals("plist.postbinary.second","added-binary-value");
    }
}
