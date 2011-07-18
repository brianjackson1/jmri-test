// JavaSoundAudioBuffer.java

package jmri.jmrit.audio;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import jmri.util.FileUtil;

/**
 * JavaSound implementation of the Audio Buffer sub-class.
 * <P>
 * For now, no system-specific implementations are forseen - this will remain
 * internal-only
 * <p>
 * For more information about the JavaSound API, visit
 * <a href="http://java.sun.com/products/java-media/sound/">http://java.sun.com/products/java-media/sound/</a>
 * 
 * <hr>
 * This file is part of JMRI.
 * <P>
 * JMRI is free software; you can redistribute it and/or modify it under
 * the terms of version 2 of the GNU General Public License as published
 * by the Free Software Foundation. See the "COPYING" file for a copy
 * of this license.
 * <P>
 * JMRI is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 * <P>
 *
 * @author Matthew Harris  copyright (c) 2009
 * @version $Revision$
 */
public class JavaSoundAudioBuffer extends AbstractAudioBuffer {

    /**
     * Holds the AudioFormat of this buffer
     */
    private transient AudioFormat _audioFormat;

    /**
     * Byte array used to store the actual data read from the file
     */
    private byte[] _dataStorageBuffer;

    /**
     * Frequency of this AudioBuffer. Used to calculate pitch changes
     */
    private int _freq;

    /**
     * Reference to the AudioInputStream used to read sound data from the file
     */
    private transient AudioInputStream _audioInputStream;

    /**
     * Holds the initialised status of this AudioBuffer
     */
    private boolean _initialised = false;

    /**
     * Constructor for new JavaSoundAudioBuffer with system name
     *
     * @param systemName AudioBuffer object system name (e.g. IAB4)
     */
    public JavaSoundAudioBuffer(String systemName) {
        super(systemName);
        if (log.isDebugEnabled()) log.debug("New JavaSoundAudioBuffer: "+systemName);
        _initialised = init();
    }

    /**
     * Constructor for new JavaSoundAudioBuffer with system name and user name
     *
     * @param systemName AudioBuffer object system name (e.g. IAB4)
     * @param userName AudioBuffer object user name
     */
    public JavaSoundAudioBuffer(String systemName, String userName) {
        super(systemName, userName);
        if (log.isDebugEnabled()) log.debug("New JavaSoundAudioBuffer: "+userName+" ("+systemName+")");
        _initialised = init();
    }

    /**
     * Performs any necessary initialisation of this AudioBuffer
     *
     * @return True if successful
     */
    private boolean init() {
        this._audioFormat = null;
        _dataStorageBuffer = null;
        this._freq = 0;
        this.setStartLoopPoint(0, false);
        this.setEndLoopPoint(0, false);
        this.setState(STATE_EMPTY);
        return true;
    }
    
    /**
     * Return reference to the DataStorageBuffer byte array
     * <p>
     * Applies only to sub-types:
     * <ul>
     * <li>Buffer
     * </u>
     * @return buffer[] reference to DataStorageBuffer
     */
    protected byte[] getDataStorageBuffer() {
        return _dataStorageBuffer;
    }

    /**
     * Retrieves the format of the sound sample stored in this buffer as an
     * AudioFormat object
     *
     * @return audio format as an AudioFormat object
     */
    protected AudioFormat getAudioFormat() {
        return _audioFormat;
    }

    @Override
    public String toString() {
        if (this.getState()!=STATE_LOADED) {
            return "Empty buffer";
        } else {
            return this.getURL() + " (" + parseFormat() + ", " + this._freq + " Hz)";
        }
    }

    protected boolean loadBuffer() {
        if (!_initialised) {
            return false;
        }

        // Reinitialise
        init();

        // Temporary storage buffer
        byte[] buffer;

        // Retrieve filename of specified .wav file
        File file = new File(FileUtil.getExternalFilename(this.getURL()));

        // Create the input stream for the audio file
        try {
            _audioInputStream = AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException ex) {
            log.error("Unsupported audio file format when loading buffer:" + ex);
            return false;
        } catch (IOException ex) {
            log.error("Error loading buffer:" + ex);
            return false;
        }

        // Get the AudioFormat
        _audioFormat = _audioInputStream.getFormat();
        this._freq = (int) _audioFormat.getSampleRate();

        // Determine the required buffer size in bytes
        // number of channels * length in frames * sample size in bits / 8 bits in a byte
        int dataSize = _audioFormat.getChannels() *
                       (int)_audioInputStream.getFrameLength() *
                       _audioFormat.getSampleSizeInBits() / 8;
        if (log.isDebugEnabled()) log.debug("Size of JavaSoundAudioBuffer (" + this.getSystemName() + ") = " + dataSize);
        if (dataSize>0) {
            // Allocate buffer space
            buffer = new byte[dataSize];

            // Load into data buffer
            int bytesRead = 0, totalBytesRead = 0;
            try {
                // Read until end of audioInputStream reached
                log.debug("Start to load JavaSoundBuffer...");
                while ((bytesRead =
                        _audioInputStream.read(buffer,
                                               totalBytesRead,
                                               buffer.length - totalBytesRead))
                                               != -1 && totalBytesRead < buffer.length)
                {
                    log.debug("read " + bytesRead + " bytes of total " + dataSize);
                    totalBytesRead += bytesRead;
                }
            } catch (IOException ex) {
                log.error("Error when reading JavaSoundAudioBuffer (" + this.getSystemName() + ") "+ ex);
                return false;
            }

            // Done. All OK.
            log.debug("...finished loading JavaSoundBuffer");
        } else {
            // Not loaded anything
            log.warn("Unable to determine length of JavaSoundAudioBuffer (" + this.getSystemName() + ")");
            log.warn(" - buffer has not been loaded.");
            return false;
        }
        
        // Done loading - need to convert byte endian order
        this._dataStorageBuffer = convertAudioEndianness(buffer, _audioFormat.getSampleSizeInBits()==16);

        // Set initial loop points
        this.setStartLoopPoint(0, false);
        this.setEndLoopPoint(_audioInputStream.getFrameLength(), false);
        this.generateLoopBuffers(LOOP_POINT_BOTH);

        this.setState(STATE_LOADED);
        if (log.isDebugEnabled()) {
            log.debug("Loaded buffer: " + this.getSystemName());
            log.debug(" from file: " + this.getURL());
            log.debug(" format: " + parseFormat() + ", " + _freq + " Hz");
            log.debug(" length: " + _audioInputStream.getFrameLength());
        }
        return true;

    }

    protected void generateLoopBuffers(int which) {
        // TODO: Actually write this bit
        //if ((which==LOOP_POINT_START)||(which==LOOP_POINT_BOTH)) {
        //}
        //if ((which==LOOP_POINT_END)||(which==LOOP_POINT_BOTH)) {
        //}
        if (log.isDebugEnabled())
            log.debug("Method generateLoopBuffers() called for JavaSoundAudioBuffer " + this.getSystemName());
    }

    protected boolean generateStreamingBuffers() {
        // TODO: Actually write this bit
        if (log.isDebugEnabled())
            log.debug("Method generateStreamingBuffers() called for JavaSoundAudioBuffer " + this.getSystemName());
        return true;
    }

    protected void removeStreamingBuffers() {
        // TODO: Actually write this bit
        if (log.isDebugEnabled())
            log.debug("Method removeStreamingBuffers() called for JavaSoundAudioBuffer " + this.getSystemName());
    }

    public int getFormat() {
        if (_audioFormat!=null) {
            if (_audioFormat.getChannels()==1 && _audioFormat.getSampleSizeInBits()==8) {
                return FORMAT_8BIT_MONO;
            } else if (_audioFormat.getChannels()==1 && _audioFormat.getSampleSizeInBits()==16) {
                return FORMAT_16BIT_MONO;
            } else if (_audioFormat.getChannels()==2 && _audioFormat.getSampleSizeInBits()==8) {
                return FORMAT_8BIT_STEREO;
            } else if (_audioFormat.getChannels()==2 && _audioFormat.getSampleSizeInBits()==16) {
                return FORMAT_16BIT_STEREO;
            } else {
                return FORMAT_UNKNOWN;
            }
        }
        return FORMAT_UNKNOWN;
    }

    /**
     * Internal method to return a string representation of the audio format
     * @return string representation
     */
    private String parseFormat() {
        switch (this.getFormat()) {
            case FORMAT_8BIT_MONO:
                return "8-bit mono";
            case FORMAT_16BIT_MONO:
                return "16-bit mono";
            case FORMAT_8BIT_STEREO:
                return "8-bit stereo";
            case FORMAT_16BIT_STEREO:
                return "16-bit stereo";
            default:
                return "unknown format";
        }
    }

    /**
     * Converts the endianness of an AudioBuffer to the format required by
     * the JRE.
     *
     * @param audioData byte array containing the read PCM data
     * @param twoByteSamples true if 16-bits per sample
     * @return byte array containing converted PCM data
     */
    private static byte[] convertAudioEndianness(byte[] audioData, boolean twoByteSamples) {

        // Create ByteBuffer for output and set endianness
        ByteBuffer out = ByteBuffer.allocate(audioData.length);
        out.order(ByteOrder.nativeOrder());

        // Wrap the audioData into a ByteBuffer for input and set endianness
        // (always Little Endian for a WAV file)
        ByteBuffer in = ByteBuffer.wrap(audioData);
        in.order(ByteOrder.LITTLE_ENDIAN);

        // Check if we have double-byte samples (i.e. 16-bit)
        if (twoByteSamples) {
            // If so, create ShortBuffer views of the in and out ByteBuffers
            // for further processing
            ShortBuffer outShort = out.asShortBuffer();
            ShortBuffer inShort = in.asShortBuffer();

            // Loop through appending data to the output buffer
            while (inShort.hasRemaining()) {
                outShort.put(inShort.get());
            }

        } else {
            // Otherwise, just loop through appending data to the output buffer
            while (in.hasRemaining()) {
                out.put(in.get());
            }
        }

        // Rewind the ByteBuffer
        out.rewind();

        // Convert output to an array if necessary
        if (!out.hasArray()) {
            // Allocate space
            byte[] array = new byte[out.capacity()];
            // fill the array
            out.get(array);
            // clear the ByteBuffer
            out.clear();

            return array;
        }

        return out.array();
    }

    protected void cleanUp() {
        if (log.isDebugEnabled()) log.debug("Cleanup JavaSoundAudioBuffer (" + this.getSystemName() + ")");
        this.dispose();
    }

    private static final long serialVersionUID = 1L;
    
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(JavaSoundAudioBuffer.class.getName());

}

/* $(#)JavaSoundAudioBuffer.java */