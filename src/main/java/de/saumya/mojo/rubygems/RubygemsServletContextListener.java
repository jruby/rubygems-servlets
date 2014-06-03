package de.saumya.mojo.rubygems;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.sonatype.nexus.ruby.ApiV1File;
import org.sonatype.nexus.ruby.DefaultLayout;
import org.sonatype.nexus.ruby.DefaultRubygemsGateway;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.cuba.DefaultRubygemsFileSystem;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.CachingStorage;
import org.sonatype.nexus.ruby.layout.HostedDELETELayout;
import org.sonatype.nexus.ruby.layout.HostedGETLayout;
import org.sonatype.nexus.ruby.layout.HostedPOSTLayout;
import org.sonatype.nexus.ruby.layout.ProxiedRubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.SimpleStorage;
import org.sonatype.nexus.ruby.layout.Storage;

public class RubygemsServletContextListener implements ServletContextListener {
    
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
                            // TODO in store.getInputStream the state get reset, instead it reset the state
                            file.set( is );
                        }
                   },
                   new HostedDELETELayout( gateway, store ) );
        } 
    }

    public void contextDestroyed( ServletContextEvent sce ) {
    }

    public void contextInitialized( ServletContextEvent sce ) {
        RubygemsGateway gateway = new DefaultRubygemsGateway();
        File proxyStorage = new File( getStorage( "GEM_PROXY_STORAGE", sce ) );
        try
        {
            Storage caching = new CachingStorage( proxyStorage, new URL( "https://rubygems.org" ) );
            try
            {
                File hostedStorage = new File( getStorage( "GEM_HOSTED_STORAGE", sce ) );
                register( sce, "proxy", new ProxiedRubygemsFileSystem( gateway, caching ) );
                register( sce, "hosted", new HostedRubygemsFileSystem( gateway,
                                                                       new SimpleStorage( hostedStorage ) ) );
            }
            catch( RuntimeException e )// no hosted configured
            {
                RubygemsFileSystem rubygems = new LegacyRubygemsFileSystem( gateway, caching );
                register( sce, RubygemsFileSystem.class.getName(), rubygems );            
            } 
        }
        catch ( IOException e )
        {
            throw new RuntimeException( "error initializing controller", e );
        }
    }

    private void register( ServletContextEvent sce,
                             String key, RubygemsFileSystem rubygems )
    {
        sce.getServletContext().setAttribute( key, rubygems );
        sce.getServletContext().log( "registered " + rubygems + " under key: " + key);
    }

    private String getStorage( String key, ServletContextEvent sce ) {
        String value = System.getenv( key );
        if(value == null){
            String pKey = key.toLowerCase().replace( "_", "." );
            value = System.getProperty( pKey );
            if(value == null){
                String iKey = pKey.replace( ".", "-" );
                value = sce.getServletContext().getInitParameter( iKey );
                if (value == null){
                    throw new RuntimeException("could not find directory location for storage:\n" +
					       "\tsystem property       : " + pKey + "\n" +
					       "\tenvironment variable  : " + key + "\n" +
					       "\tcontext init parameter: " + iKey + "\n");
                }
            }
        }
        sce.getServletContext().log( key + " resolved to " + value ); 
        return value;
    }

}
