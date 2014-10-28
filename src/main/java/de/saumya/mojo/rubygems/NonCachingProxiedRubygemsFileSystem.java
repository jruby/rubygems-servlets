package de.saumya.mojo.rubygems;

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.ruby.DependencyData;
import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.GemArtifactFile;
import org.sonatype.nexus.ruby.GemFile;
import org.sonatype.nexus.ruby.GemspecFile;
import org.sonatype.nexus.ruby.PomFile;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.Sha1File;
import org.sonatype.nexus.ruby.cuba.DefaultRubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.DELETELayout;
import org.sonatype.nexus.ruby.layout.DefaultLayout;
import org.sonatype.nexus.ruby.layout.ProxiedGETLayout;
import org.sonatype.nexus.ruby.layout.ProxyStorage;

public class NonCachingProxiedRubygemsFileSystem extends DefaultRubygemsFileSystem
{
    
    static class NonCachingGETLayout extends ProxiedGETLayout 
    {

        private final DefaultLayout layout;
        private final ProxyStorage store;

        public NonCachingGETLayout( RubygemsGateway gateway, ProxyStorage store, DefaultLayout layout )
        {
            super( gateway, store );
            // the raw layout without store interaction
            this.layout = layout;
            // need to access it from here
            this.store = store;
        }

        @Override
        public Sha1File sha1( RubygemsFile file )
        {
            if ( file.type() == FileType.GEM_ARTIFACT )
            {
                Sha1File sha = super.sha1( file );
                store.retrieve( sha );
                if ( sha.exists() )
                {
                    // OK already saved locally
                    return sha;
                }
                GemFile gem = ((GemArtifactFile) file).gem( null );
                try
                {
                    // download the gem file
                    store.retrieve( gem );
                    // calculate the sha of this gem
                    Sha1File gemSha = super.sha1( gem );
                    try (InputStream is = store.getInputStream( gemSha ) ) {
                        // store it locally as sha for the gem-artifact
                        store.create( is, sha );
                    }
                }
                catch (IOException e)
                {
                    sha.setException( e );
                }
                finally
                {
                    // we do not want to cache the gem file
                    store.delete( gem ) ;
                }
                return sha;
            }
            else
            {
                return super.sha1( file );
            }
        }
        
        @Override
        public GemFile gemFile( String name, String version, String platform )
        {
            // just the raw file without store interaction
            return layout.gemFile( name, version, platform );
        }

        @Override
        protected void setPomPayload(PomFile pom, boolean snapshot) {
          store.retrieve( pom );
          if ( pom.exists() )
          {
            // we have cache pom.xml
            return;
          }
          GemFile gem = null;
          try {
            DependencyData dependencies = newDependencyData(pom.dependency());
            if ("java".equals(dependencies.platform(pom.version()))) {
              gem = pom.gem( dependencies );
              store.retrieve( gem );
              super.setPomPayload( pom, snapshot );
              // cache the pom.xml for later retrievals 
              try
              {
                store.create( store.getInputStream( pom ), pom );
              }
              catch (IOException e)
              {
                pom.setException( e );
              }
            }
            else
            {
              super.setPomPayload( pom, snapshot );
            }
          }
          catch (IOException e) {
            pom.setException(e);
          }
          finally {
            if (gem != null) {
              // we cached the pom.xml so no need to store the gem any longer
              store.delete(gem);
            }
          }
        }

        @Override
        protected void setGemArtifactPayload( GemArtifactFile file )
        {
            // the difference to super is NOT to retrieve the gem !
            try
            {   
                GemFile gem = file.gem( newDependencyData( file.dependency() ) );
                if ( gem == null )
                {
                    file.markAsNotExists();
                }
                else
                {
                    file.set( gem.get() );
                }
            }
            catch (IOException e)
            {
                file.setException( e );
            }
        }
        
    }
    
    public NonCachingProxiedRubygemsFileSystem( RubygemsGateway gateway, ProxyStorage store )
    {
        this( gateway, store, new DefaultLayout() );
    }
    
    public NonCachingProxiedRubygemsFileSystem( RubygemsGateway gateway, ProxyStorage store, DefaultLayout layout )
    {
        super( layout,
               new NonCachingGETLayout( gateway, store, layout ),
               null, // no POST allowed
               new DELETELayout( gateway, store ) );
    }
}
