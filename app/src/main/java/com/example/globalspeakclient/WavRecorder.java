package com.example.globalspeakclient;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * Handles audio recording and WAV file creation.
 * This class provides:
 * - Initialization of an `AudioRecord` instance for capturing PCM audio.
 * - Recording and saving raw PCM data to a temporary file.
 * - Converting PCM data to a WAV file with the correct header.
 * The recorded WAV file can be retrieved using `getWavFile()`.
 */
public class WavRecorder {
    private static final String TAG = "WavRecorder";

    private AudioRecord audioRecord;
    private int bufferSize;
    private boolean isRecording = false;
    private File rawFile;
    private File wavFile;

    public WavRecorder(int sampleRate, int channelConfig, int audioFormat, File wavFile) {
        // Buffer size calculation
        this.bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        // Ensure the buffer size is valid
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            throw new IllegalArgumentException("Invalid buffer size calculated for AudioRecord");
        }

        // AudioRecord initialization
        try {
            this.audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
            );
        } catch (SecurityException e) {
            // This error will only occur if the caller didn't check permissions
            throw new IllegalStateException("Missing RECORD_AUDIO permission. Ensure permissions are granted before initializing WavRecorder.", e);
        }

        // Set up the temporary raw and output WAV files
        this.rawFile = new File(wavFile.getParent(), "temp_audio.raw");
        this.wavFile = wavFile;
    }


    public void startRecording() {
        audioRecord.startRecording();
        isRecording = true;

        new Thread(this::writePcmData).start();
    }

    public void stopRecording() {
        isRecording = false;
        if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            audioRecord.stop();
            audioRecord.release();
        }
        convertPcmToWav();
    }

    private void writePcmData() {
        try (FileOutputStream fos = new FileOutputStream(rawFile)) {
            byte[] buffer = new byte[bufferSize];
            while (isRecording) {
                int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing PCM data", e);
        }
    }

    private void convertPcmToWav() {
        try (FileInputStream fis = new FileInputStream(rawFile);
             FileOutputStream fos = new FileOutputStream(wavFile)) {

            long audioLength = rawFile.length();
            long dataLength = audioLength + 36;
            int sampleRate = 44100;
            int channels = 1;
            int byteRate = sampleRate * channels * 16 / 8;

            writeWavHeader(fos, audioLength, dataLength, sampleRate, channels, byteRate);

            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }

            rawFile.delete(); // Cleanup temporary raw file
        } catch (IOException e) {
            Log.e(TAG, "Error converting PCM to WAV", e);
        }
    }

    private void writeWavHeader(FileOutputStream out, long audioLength, long dataLength,
                                int sampleRate, int channels, int byteRate) throws IOException {

        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (dataLength & 0xff);
        header[5] = (byte) ((dataLength >> 8) & 0xff);
        header[6] = (byte) ((dataLength >> 16) & 0xff);
        header[7] = (byte) ((dataLength >> 24) & 0xff);
        header[8] = 'W'; // WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // fmt
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // Subchunk1Size (PCM)
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // AudioFormat (PCM)
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * 2); // Block align
        header[33] = 0;
        header[34] = 16; // Bits per sample
        header[35] = 0;
        header[36] = 'd'; // data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (audioLength & 0xff);
        header[41] = (byte) ((audioLength >> 8) & 0xff);
        header[42] = (byte) ((audioLength >> 16) & 0xff);
        header[43] = (byte) ((audioLength >> 24) & 0xff);

        out.write(header, 0, 44);
    }
    public File getWavFile() {
        return wavFile;
    }
}
