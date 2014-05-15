package de.saumya.mojo.rubygems;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.sonatype.nexus.ruby.DefaultRubygemsGateway;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.CachingStorage;
import org.sonatype.nexus.ruby.layout.HostedRubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.ProxiedRubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.SimpleStorage;
import org.sonatype.nexus.ruby.layout.Storage;

public class RubygemsServletContextListener implements ServletContextListener {

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
                register( sce, "hosted", new HostedRubygemsFileSystem( gateway, new SimpleStorage( hostedStorage ) ) );
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
        return value;
    }

}
