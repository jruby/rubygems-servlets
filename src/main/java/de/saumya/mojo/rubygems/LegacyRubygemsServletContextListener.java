package de.saumya.mojo.rubygems;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jruby.embed.ScriptingContainer;
import org.sonatype.nexus.ruby.DefaultRubygemsGateway;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.cuba.RubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.CachingProxyStorage;
import org.sonatype.nexus.ruby.layout.ProxyStorage;
import org.sonatype.nexus.ruby.layout.Storage;

public class LegacyRubygemsServletContextListener extends AbstractRubygemsServletContextListener
{

    static class LegacyRubygemsFileSystem extends NonCachingProxiedRubygemsFileSystem
    {

        public LegacyRubygemsFileSystem( RubygemsGateway gateway, ProxyStorage store )
        {
            super( gateway, store );
        }

        @Override
        public RubygemsFile get( String original, String query )
        {
            return super.get( "/maven" + original, query );
        }

        @Override
        public RubygemsFile post( InputStream is, String path )
        {
            throw new RuntimeException( "not supported" );
        }

        @Override
        public RubygemsFile delete( String original )
        {
            throw new RuntimeException( "not supported" );
        }
    }

    public void doContextInitialized( Helper configor ) throws IOException 
    {
        // TODO use IsolatedScriptingContainer
        RubygemsGateway gateway = new DefaultRubygemsGateway(new ScriptingContainer());
        File path = configor.getFile( "GEM_PROXY_STORAGE", "var/cache/rubygems/proxy" );
        if ( path == null )
        {
            throw new RuntimeException( "no storage path given");
        }
        ProxyStorage storage = new CachingProxyStorage( path, new URL( "https://rubygems.org" ) );
        RubygemsFileSystem rubygems = new LegacyRubygemsFileSystem( gateway, storage );
        configor.register( RubygemsFileSystem.class.getName(), null, rubygems );
        configor.register( Storage.class.getName(), null, storage );
    }

}
