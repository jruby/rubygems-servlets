package de.saumya.mojo.rubygems;

import java.io.IOException;

import org.sonatype.nexus.ruby.DefaultLayout;
import org.sonatype.nexus.ruby.FileType;
import org.sonatype.nexus.ruby.GemArtifactFile;
import org.sonatype.nexus.ruby.GemFile;
import org.sonatype.nexus.ruby.RubygemsFile;
import org.sonatype.nexus.ruby.RubygemsGateway;
import org.sonatype.nexus.ruby.Sha1File;
import org.sonatype.nexus.ruby.cuba.DefaultRubygemsFileSystem;
import org.sonatype.nexus.ruby.layout.DELETELayout;
import org.sonatype.nexus.ruby.layout.GETLayout;
import org.sonatype.nexus.ruby.layout.Storage;

public class NonCachingProxiedRubygemsFileSystem extends DefaultRubygemsFileSystem
{
    
    static class NonCachingGETLayout extends GETLayout 
    {

        private final DefaultLayout layout;

        public NonCachingGETLayout( RubygemsGateway gateway, Storage store, DefaultLayout layout )
        {
            super( gateway, store );
            // the raw layout without store interaction
            this.layout = layout;
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
                    // store it locally as sha for the gem-artifact
                    store.create( store.getInputStream( gemSha ), sha );
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
        protected void setGemArtifactPayload( GemArtifactFile file )
        {
            // the difference to super is NOT to retrieve the gem !
            try
            {   
                GemFile gem = file.gem( newDependencies( file.dependency() ) );
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
    
    public NonCachingProxiedRubygemsFileSystem( RubygemsGateway gateway, Storage store )
    {
        this( gateway, store, new DefaultLayout() );
    }
    
    public NonCachingProxiedRubygemsFileSystem( RubygemsGateway gateway, Storage store, DefaultLayout layout )
    {
        super( layout,
               new NonCachingGETLayout( gateway, store, layout ),
               null, // no POST allowed
               new DELETELayout( gateway, store ) );
    }
}
