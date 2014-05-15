package de.saumya.mojo.rubygems;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sonatype.nexus.ruby.IOUtil;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;

public class RubygemsServlet extends HttpServlet
{
    private static final long serialVersionUID = -6942659125767757561L;

    private RubygemsFileSystem fileSystem;

    @Override
    public void init() throws ServletException {
        super.init();
        
        this.fileSystem = (RubygemsFileSystem) getServletContext().getAttribute( getServletConfig().getServletName() );
        if ( fileSystem == null )
        {
            this.fileSystem = (RubygemsFileSystem) getServletContext().getAttribute(RubygemsFileSystem.class.getName());
        }
    }

    protected void handle( HttpServletRequest req, HttpServletResponse resp, RubygemsFile file ) 
            throws IOException, ServletException
    {
        switch( file.state() )
        {
        case FORBIDDEN:
            resp.sendError( HttpServletResponse.SC_FORBIDDEN );
            break;
        case NOT_EXISTS:
            resp.sendError( HttpServletResponse.SC_NOT_FOUND );            
            break;
        case NO_PAYLOAD:
            resp.sendError( HttpServletResponse.SC_NOT_FOUND, 
                            req.getRequestURI() + " has no view - not implemented" );          
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
    
    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
    {
        log( req.getPathInfo() );
        handle( req, resp, 
                fileSystem.get( req.getPathInfo(), req.getQueryString() ) );
    }

    @Override
    protected void doPost( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
    {
        handle( req, resp, 
                fileSystem.post( req.getInputStream(), req.getPathInfo() ) );
    }

    @Override
    protected void doDelete( HttpServletRequest req, HttpServletResponse resp )
            throws ServletException, IOException
    {
        handle( req, resp, 
                fileSystem.delete( req.getPathInfo() ) );
    }
    
}