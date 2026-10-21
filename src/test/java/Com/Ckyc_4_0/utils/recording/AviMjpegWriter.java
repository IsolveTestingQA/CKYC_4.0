/**
 * CKYC 4.0 Automation Framework
 * Author : Aravindhan
 * Created and developed by Aravindhan
 */
package Com.Ckyc_4_0.utils.recording;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes a Motion-JPEG AVI (one JPEG per frame) with no external library. Plays in Windows Media Player, VLC and most
 * players. Classic AVI limit: the file is kept below about 1.9 GB ({@link #isFull()}).
 */
final class AviMjpegWriter implements AutoCloseable {

	private static final long MAX_BYTES = 1_900_000_000L;

	private final RandomAccessFile out;
	private final int width;
	private final int height;
	private final int fps;
	private final List<int[]> index = new ArrayList<>(); // {offset relative to 'movi', size}
	private long moviStart; // file position of the 'movi' fourcc
	private long moviSizePos;
	private long avihFramesPos;
	private long strhLengthPos;
	private long maxFrame;
	private boolean closed;

	AviMjpegWriter(File file, int width, int height, int fps) throws IOException {
		this.out = new RandomAccessFile(file, "rw");
		this.out.setLength(0);
		this.width = width;
		this.height = height;
		this.fps = Math.max(1, fps);
		writeHeaders();
	}

	private void writeHeaders() throws IOException {
		out.writeBytes("RIFF");
		writeInt(0); // patched on close
		out.writeBytes("AVI ");

		out.writeBytes("LIST");
		writeInt(4 + 8 + 56 + 8 + 4 + 8 + 56 + 8 + 40);
		out.writeBytes("hdrl");

		out.writeBytes("avih");
		writeInt(56);
		writeInt(1_000_000 / fps); // microseconds per frame
		writeInt(0); // max bytes per second
		writeInt(0); // padding granularity
		writeInt(0x10); // AVIF_HASINDEX
		avihFramesPos = out.getFilePointer();
		writeInt(0); // total frames (patched)
		writeInt(0); // initial frames
		writeInt(1); // streams
		writeInt(0); // suggested buffer size
		writeInt(width);
		writeInt(height);
		writeInt(0);
		writeInt(0);
		writeInt(0);
		writeInt(0);

		out.writeBytes("LIST");
		writeInt(4 + 8 + 56 + 8 + 40);
		out.writeBytes("strl");

		out.writeBytes("strh");
		writeInt(56);
		out.writeBytes("vids");
		out.writeBytes("MJPG");
		writeInt(0); // flags
		writeShort(0); // priority
		writeShort(0); // language
		writeInt(0); // initial frames
		writeInt(1); // scale
		writeInt(fps); // rate
		writeInt(0); // start
		strhLengthPos = out.getFilePointer();
		writeInt(0); // length in frames (patched)
		writeInt(0); // suggested buffer size
		writeInt(-1); // quality
		writeInt(0); // sample size
		writeShort(0);
		writeShort(0);
		writeShort(width);
		writeShort(height);

		out.writeBytes("strf");
		writeInt(40);
		writeInt(40); // BITMAPINFOHEADER size
		writeInt(width);
		writeInt(height);
		writeShort(1); // planes
		writeShort(24); // bit count
		out.writeBytes("MJPG");
		writeInt(width * height * 3);
		writeInt(0);
		writeInt(0);
		writeInt(0);
		writeInt(0);

		out.writeBytes("LIST");
		moviSizePos = out.getFilePointer();
		writeInt(0); // patched
		moviStart = out.getFilePointer();
		out.writeBytes("movi");
	}

	boolean isFull() {
		try {
			return out.getFilePointer() > MAX_BYTES;
		} catch (IOException e) {
			return true;
		}
	}

	int frames() {
		return index.size();
	}

	void addFrame(byte[] jpeg) throws IOException {
		if (closed || jpeg == null || jpeg.length == 0 || isFull()) {
			return;
		}
		long chunkStart = out.getFilePointer();
		out.writeBytes("00dc");
		writeInt(jpeg.length);
		out.write(jpeg);
		if ((jpeg.length & 1) == 1) {
			out.write(0);
		}
		index.add(new int[] { (int) (chunkStart - moviStart), jpeg.length });
		maxFrame = Math.max(maxFrame, jpeg.length);
	}

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}
		closed = true;
		try {
			long moviEnd = out.getFilePointer();
			out.writeBytes("idx1");
			writeInt(index.size() * 16);
			for (int[] e : index) {
				out.writeBytes("00dc");
				writeInt(0x10); // AVIIF_KEYFRAME
				writeInt(e[0]);
				writeInt(e[1]);
			}
			long end = out.getFilePointer();
			patch(4, (int) (end - 8));
			patch(moviSizePos, (int) (moviEnd - moviStart));
			patch(avihFramesPos, index.size());
			patch(strhLengthPos, index.size());
			patch(avihFramesPos + 12, (int) maxFrame); // suggested buffer size in avih
			patch(strhLengthPos + 4, (int) maxFrame);
		} finally {
			out.close();
		}
	}

	private void patch(long pos, int value) throws IOException {
		long keep = out.getFilePointer();
		out.seek(pos);
		writeInt(value);
		out.seek(keep);
	}

	private void writeInt(int v) throws IOException {
		out.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array());
	}

	private void writeShort(int v) throws IOException {
		out.write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) v).array());
	}
}
