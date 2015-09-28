package de.saumya.mojo.rubygems;

import org.sonatype.nexus.ruby.Directory;

import java.util.Arrays;

public class HtmlDirectoryBuilder  {
    
    private StringBuilder html = new StringBuilder("<!DOCTYPE html>\n");
    
    public HtmlDirectoryBuilder( String root, Directory file )
    {
        buildHeader(root + file.remotePath());
        String[] items = file.getItems();
        Arrays.sort(items);
        build(items);
        buildFooter();
    }

    public String toHTML(){
        return html.toString();
    }

    public void buildHeader(String title) {
        html.append("<html>\n");
        html.append("  <header>\n");
        html.append("    <title>").append(title).append("</title>\n");
        html.append("  </header>\n");
        html.append("  <body>\n");
        html.append("    <h1>Index of ").append(title).append("</h1>\n");
        html.append("    <hr />\n");
        html.append("    <a href=\"..\">../</a><br />\n");
    }

    public void buildFooter() {
        html.append("  <hr />\n");
        html.append("  </body>\n");
        html.append("</html>\n");
    }
    
    public void build(String... items) {
        for( String item : items ) {
            buildLink( item );
        }
    }
    
    public void buildLink(String name) {
        html.append("    <a href=\"").append(name).append("\">").append(name).append("</a><br />\n");
    }
    
}