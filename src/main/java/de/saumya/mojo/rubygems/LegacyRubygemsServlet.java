package de.saumya.mojo.rubygems;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.GemArtifactFile;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.Storage;

public class LegacyRubygemsServlet extends RubygemsServlet
{
    private static final long serialVersionUID = 2441264980328145654L;

    private static final String RUBYGEMS_DOWNLOAD_URL = "http://rubygems.org/gems/";
 
    @Override
    public void init() throws ServletException {
        super.init();
        
        this.fileSystem = (RubygemsFileSystem) getServletContext().getAttribute( RubygemsFileSystem.class.getName() );
        this.storage = (Storage) getServletContext().getAttribute( Storage.class.getName());
    }
    
    protected void handle( HttpServletRequest req, HttpServletResponse resp, RubygemsFile file ) 
            throws IOException, ServletException
    {
        if ( file.type() == FileType.GEM_ARTIFACT )
        {
            // use a non https url here since IVY can not handle
            // redirects from https to http
            // https://github.com/torquebox/rubygems-servlets/issues/11
            URL url = new URL("https://rubygems.org/gems/" + ((GemArtifactFile) file ).gem( null ).filename() + ".gem" );
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("HEAD");
            con.setInstanceFollowRedirects(false);
            String location = con.getHeaderField("Location");
            if (location == null) {
                // use the old hardcoded storage as fallback
                location = RUBYGEMS_DOWNLOAD_URL + ((GemArtifactFile) file ).gem( null ).filename() + ".gem";
            }
            resp.sendRedirect( location );
        }
        else
        {
            super.handle( req, resp, file );
        }
    }
}
