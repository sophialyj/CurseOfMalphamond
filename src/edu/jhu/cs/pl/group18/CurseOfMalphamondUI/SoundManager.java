/**
 * 
 */
package edu.jhu.cs.pl.group18.CurseOfMalphamondUI;
 
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.util.WaveData;
 
 
/**
 * <p>
 * Simple sound manager for OpenAL using n sources accessed in
 * a round robin schedule. Source n is reserved for a single buffer and checking for
 * whether it's playing.
 * This code piece is modified by us and originally come from the lwjgl tutorial.
 * </p>
 @author Yijie Li
 */
public class SoundManager {
 
  /** We support at most 256 buffers*/
  private int[] buffers = new int[256];
 
  /** Number of sources is limited tby user (and hardware) */
  private int[] sources;
 
  /** Our internal scratch buffer */
  private IntBuffer scratchBuffer = BufferUtils.createIntBuffer(256);
 
  /** Whether we're running in no sound mode */
  private boolean soundOutput;
 
  /** Current index in our buffers */
  private int bufferIndex;
 
  /** Current index in our source list */
  private int sourceIndex;
 
  /**
   * Creates a new SoundManager
   */
  public SoundManager() {
  }
 
  /**
   * Plays a sound effect
   * @param buffer Buffer index to play gotten from addSound
   */
  public void playEffect(int buffer) {
    if(soundOutput) {
      // make sure we never choose last channel, since it is used for special sounds
        int channel = sources[(sourceIndex++ % (sources.length-1))];
 
      // link buffer and source, and play it
        AL10.alSourcei(channel, AL10.AL_BUFFER, buffers[buffer]);
        AL10.alSourcePlay(channel);
    }
  }
 
  /**
   * Plays a sound on last source
   * @param buffer Buffer index to play gotten from addSound
   */
  public void playSound(int buffer) {
    if(soundOutput) {
      AL10.alSourcei(sources[sources.length-1], AL10.AL_BUFFER, buffers[buffer]);
      AL10.alSourcePlay(sources[sources.length-1]);
    }
  }
 
  /**
   * Whether a sound is playing on last source
   * @return true if a source is playing right now on source n
   */
  public boolean isPlayingSound() {
    return AL10.alGetSourcei(sources[sources.length-1], AL10.AL_SOURCE_STATE) == AL10.AL_PLAYING;
  }
 
  /**
   * Initializes the SoundManager
   *
   * @param channels Number of channels to create
   */
  public void initialize(int channels) {
    try {
     AL.create();
 
     // allocate sources
     scratchBuffer.limit(channels);
     AL10.alGenSources(scratchBuffer);
     scratchBuffer.rewind();
     scratchBuffer.get(sources = new int[channels]);
 
     // could we allocate all channels?
     if(AL10.alGetError() != AL10.AL_NO_ERROR) {
        throw new LWJGLException("Unable to allocate " + channels + " sources");
     }
 
     // we have sound
     soundOutput = true;
    } catch (LWJGLException le) {
        le.printStackTrace();
      //System.out.println("Sound disabled");
    }
  }
 
  /**
   * Adds a sound to the Sound Managers pool
   *
   * @param path Path to file to load
   * @return index into SoundManagers buffer list
 * @throws FileNotFoundException 
   */
  public int addSound(String path) throws FileNotFoundException {
    // Generate 1 buffer entry
    scratchBuffer.rewind().position(0).limit(1);
    AL10.alGenBuffers(scratchBuffer);
    buffers[bufferIndex] = scratchBuffer.get(0);
 
    // load wave data from buffer
    WaveData wavefile = WaveData.create(new BufferedInputStream(new FileInputStream(path)));
 
    // copy to buffers
    AL10.alBufferData(buffers[bufferIndex], wavefile.format, wavefile.data, wavefile.samplerate);
 
    // unload file again
    wavefile.dispose();
 
    // return index for this sound
    return bufferIndex++;
  }
 
  /**
   * Destroy this SoundManager
   */
  public void destroy() {
    if(soundOutput) {
 
      // stop playing sounds
      scratchBuffer.position(0).limit(sources.length);
      scratchBuffer.put(sources).flip();
      AL10.alSourceStop(scratchBuffer);
 
      // destroy sources
      AL10.alDeleteSources(scratchBuffer);
 
      // destroy buffers
      scratchBuffer.position(0).limit(bufferIndex);
      scratchBuffer.put(buffers, 0, bufferIndex).flip();
      AL10.alDeleteBuffers(scratchBuffer);
 
      // destory OpenAL
      AL.destroy();
    }
  }
}
