package de.saumya.mojo.rubygems;

import java.io.IOException;

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

    private static final String RUBYGEMS_S3_URL = "http://s3.amazonaws.com/production.s3.rubygems.org/gems/";
 
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
            resp.sendRedirect( RUBYGEMS_S3_URL + ((GemArtifactFile) file ).gem( null ).filename() + ".gem" );
        }
        else
        {
            super.handle( req, resp, file );
        }
    }
}
