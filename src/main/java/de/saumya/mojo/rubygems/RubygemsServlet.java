package de.saumya.mojo.rubygems;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.ruby.Directory;
import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.GemArtifactFile;
import org.sonatype.nexus.ruby.IOUtil;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;

public class RubygemsServlet extends HttpServlet
{
    private static final long serialVersionUID = -6942659125767757561L;

    RubygemsFileSystem fileSystem;

    @Override
    public void init() throws ServletException {
        super.init();
        
        this.fileSystem = (RubygemsFileSystem) getServletContext().getAttribute( getServletConfig().getServletName() );
    }

    protected void handle( HttpServletRequest req, HttpServletResponse resp, RubygemsFile file ) 
            throws IOException, ServletException
    {
        log( getPathInfo( req ) + " - " + file ); 
        switch( file.state() )
        {
        case FORBIDDEN:
            resp.sendError( HttpServletResponse.SC_FORBIDDEN );
            break;
        case NOT_EXISTS:
            resp.sendError( HttpServletResponse.SC_NOT_FOUND );            
            break;
        case NO_PAYLOAD:
            switch( file.type() )
            {
            case DIRECTORY:
                writeOutDirectory( resp, (Directory) file );
                break;
            case GEM_ARTIFACT:
                // we can pass in null as dependenciesData since we have already the gem
                resp.sendRedirect( "https://rubygems.org/gems/" + ((GemArtifactFile) file ).gem( null ).filename() + ".gem" );
                return;
            case GEM:
//            case GEMSPEC:
                resp.sendRedirect( "https://rubygems.org/" + file.remotePath() );
                return;
            default:
                resp.sendError( HttpServletResponse.SC_NOT_FOUND, 
                                req.getRequestURI() + " has no view - not implemented" );          
            }
            break;
        case ERROR:
            throw new ServletException( file.getException() );
        case TEMP_UNAVAILABLE:
            resp.setHeader("Retry-After", "120");//seconds
            resp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE );           
            break;
        case PAYLOAD:
            resp.setContentType( file.type().mime() );
            if ( file.type().encoding() != null )
            {
                resp.setCharacterEncoding( file.type().encoding() );
            }
            if( file.type().isVaryAccept() )
            {
                resp.setHeader("Vary", "Accept");
            }
            IOUtil.copy( (InputStream) file.get(), resp.getOutputStream() );
            break;
        case NEW_INSTANCE:
            throw new ServletException( "BUG: should never reach here" );
        }
    }

    private void writeOutDirectory( HttpServletResponse resp, Directory file ) throws IOException
    {
        HtmlDirectoryBuilder html = new HtmlDirectoryBuilder( getServletConfig().getServletName(), file );
        resp.setContentType( "text/html" );
        resp.setCharacterEncoding( "utf-8" );
        resp.getWriter().print( html.toHTML() );
    }

    private String getPathInfo( HttpServletRequest req )
    {
        String path = req.getPathInfo();
        if ( path == null )
        {
            return "";
        }
        else
        {
            return path;
        }
    }

    
    @Override
    protected void service( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
    {
        if ( fileSystem == null )
        {
            resp.sendError( HttpServletResponse.SC_SERVICE_UNAVAILABLE, "not configured to server requests - maybe wrong base-URL !?" );
        }
        else
        {
            super.service( req, resp );
        }
    }

    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
    {
        RubygemsFile file = fileSystem.get( getPathInfo( req ), req.getQueryString() );
        if ( file.type() == FileType.API_V1 && "api_key".equals( file.name() ) ) 
        {
            log( getPathInfo( req ) );
            resp.getOutputStream().print( "behappy" );
            resp.setContentLength( 7 );
            resp.setContentType( "text/plain" );
        }
        else
        {
            handle( req, resp, file );
        }
    }

    @Override
    protected void doPost( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
    {
        handle( req, resp, fileSystem.post( req.getInputStream(), getPathInfo( req ) ) );
    }

    @Override
    protected void doDelete( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
    {
        handle( req, resp, fileSystem.delete( getPathInfo( req ) ) );
    }
    
}
