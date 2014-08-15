package de.saumya.mojo.rubygems;

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.ruby.DefaultRubygemsGateway;
import org.sonatype.nexus.ruby.Layout;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.cuba.DefaultRubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.CachingStorage;
import org.sonatype.nexus.ruby.layout.GETLayout;
import org.sonatype.nexus.ruby.layout.ProxiedRubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.ProxyStorage;
import org.sonatype.nexus.ruby.layout.SimpleStorage;
import org.sonatype.nexus.ruby.layout.Storage;

public class RubygemsServletContextListener extends AbstractRubygemsServletContextListener 
{
    
    public void doContextInitialized( Helper configor ) throws IOException
    {
        RubygemsGateway gateway = new DefaultRubygemsGateway();
        File file = configor.getFile( "GEM_PROXY_STORAGE" );
        if ( file != null )
        {
            Storage storage = new CachingStorage( file, configor.getURL( "GEM_PROXY_URL" ) );
            configor.register( "proxy", storage, new NonCachingProxiedRubygemsFileSystem( gateway, storage ) );
        }
        
        file = configor.getFile( "GEM_CACHING_PROXY_STORAGE" );
        if ( file != null )
        {
            ProxyStorage storage = new CachingStorage( file, configor.getURL( "GEM_CACHING_PROXY_URL" ) );
            configor.register( "caching", storage, new ProxiedRubygemsFileSystem( gateway, storage ) );
        }
        
        file = new File( configor.getConfigValue( "GEM_HOSTED_STORAGE" ) );
        if ( file != null )
        {
            Storage storage =  new SimpleStorage( file );
            configor.register( "hosted", storage, new HostedRubygemsFileSystem( gateway, storage ) );
        }
        
        if ( "true".equals( configor.getConfigValue( "GEM_MERGED" ) ) )
        {
            Storage storage = new MergedSimpleStorage( gateway, configor.storages );
            Layout layout = new GETLayout( gateway, storage );
            configor.register( "merged", new DefaultRubygemsFileSystem( layout,// get requests 
                                                                        null,// no post requests 
                                                                        null ) );// no delete requests
        }
    }

}
