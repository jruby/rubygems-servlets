package de.saumya.mojo.rubygems;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.util.log.StdErrLog;
import org.sonatype.nexus.ruby.ApiV1File;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.cuba.DefaultRubygemsFileSystem;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.DefaultLayout;
import org.sonatype.nexus.ruby.layout.HostedDELETELayout;
import org.sonatype.nexus.ruby.layout.HostedGETLayout;
import org.sonatype.nexus.ruby.layout.HostedPOSTLayout;
import org.sonatype.nexus.ruby.layout.Storage;

public abstract class AbstractRubygemsServletContextListener implements ServletContextListener
{

    public static class HostedRubygemsFileSystem extends DefaultRubygemsFileSystem
    {
        public HostedRubygemsFileSystem( RubygemsGateway gateway,
                                         Storage store )
        {
            super( new DefaultLayout(),
                   new HostedGETLayout( gateway, store ),
                   new HostedPOSTLayout( gateway, store ) {
                        
                        // monkey patch upstream
                
                        @Override
                        public ApiV1File apiV1File( String name )
                        {
                            ApiV1File apiV1 = super.apiV1File( name );
                            if ( "api_key".equals( apiV1.name() ) )
                            {
                                apiV1.markAsForbidden();
                            }
                            else
                            {
                                apiV1.resetState();
                                apiV1.set( null );
                            }
                            return apiV1;
                        }
    
                        @Override
                        public void addGem( InputStream is, RubygemsFile file )
                        {
                            super.addGem( is, file );
                            // TODO in store.getInputStream the state get reset, 
                            // instead of setting the state correctly
                            file.set( is );
                        }
                   },
                   new HostedDELETELayout( gateway, store ) );
        } 
    }

    public void contextDestroyed( ServletContextEvent sce )
    {
    }

    public void contextInitialized( ServletContextEvent sce )
    {
        try
        {
            doContextInitialized( new Helper( sce ) );
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "error initializing controller", e );
        }
    }
    
    public static class Helper
    {
        final ServletContextEvent sce;
        final List<Storage> storages = new ArrayList<Storage>();
        
        Helper( ServletContextEvent sce )
        {
            this.sce = sce;
        }
        
        protected File getFile( String key, String defaultValue )
        {
            String path = getConfigValue( key, defaultValue );
            if ( path == null || path.isEmpty() )
            {
                return null;
            }
            return new File( path );
        }

        protected URL getURL( String key, String defaultValue ) throws MalformedURLException
        {
            String url = getConfigValue( key, defaultValue );
            if ( url == null )
            {
                throw new RuntimeException( "no url given");
            }
            return new URL( url );
        }

        protected void addStorageAndRegister( String key, Storage storage, RubygemsFileSystem rubygems )
        {
            this.storages.add( storage );
            register(key, RubygemsFileSystem.class, rubygems);
            register(key, Storage.class, storage);
        }
        
        protected void register( String key, Class<?> type, Object rubygems )
        {
            if ( type != null )
            {
                key = key + "/" + type.getName();
            }
            sce.getServletContext().setAttribute(key, rubygems);
            sce.getServletContext().log("registered " + rubygems + " under key: " + key);
        }

        protected String getConfigValue( String key, String defaultValue )
        {
            String value = System.getenv( key );
            if(value == null){
                String pKey = key.toLowerCase().replace( "_", "." );
                value = System.getProperty( pKey );
                if(value == null){
                    String iKey = pKey.replace( ".", "-" );
                    value = sce.getServletContext().getInitParameter( iKey );
                    if (value == null){
                        if (defaultValue != null) {
                            value = defaultValue;
                        }
                        else {
                            String message = "could not find config: " +
                                    "system property( " + pKey + " ) - " +
                                    "environment variable( " + key + " ) - " +
                                    "context init parameter( " + iKey + " )";
                            sce.getServletContext().log(message);
                            return null;
                        }
                    }
                }
            }
            if (value == null || value.length() == 0) {
                sce.getServletContext().log(key + " not found");
            }
            else {
                sce.getServletContext().log(key + " resolved to " + value);
            }
            return value;
        }
    }
    
    abstract protected void doContextInitialized( Helper configor ) throws IOException;

}