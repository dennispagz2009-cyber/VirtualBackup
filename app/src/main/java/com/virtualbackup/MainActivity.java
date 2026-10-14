package com.virtualbackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.virtualbackup.R;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener {
	public static final String TAG = "VirtualBackup";
	
	private String dir;
	private TextView status;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		dir = Environment.getExternalStorageDirectory() + "/VirtualBackup";
		
		TextView t = findViewById(R.id.info);
		t.setText(getResources().getString(R.string.backup_dir) + ": " + dir + "\n\n" + getFilesDir());
		
		status = findViewById(R.id.status);
		
		Button backup = findViewById(R.id.backup);
		backup.setOnClickListener(this);
		
		Button restore = findViewById(R.id.restore);
		restore.setOnClickListener(this);
		
		if (Build.VERSION.SDK_INT >= 23) {
			requestPermissions(new String[]{
				Manifest.permission.READ_EXTERNAL_STORAGE,
				Manifest.permission.WRITE_EXTERNAL_STORAGE,
				"android.permission.WRITE_MEDIA_STORAGE",
			}, 0);
		}
	}

	@Override
	public void onClick(View v) {
		final boolean restore = v.getId() == R.id.restore;
		
		final String files = getFilesDir().getAbsolutePath();
		final String own = getPackageName();
		
		int pos = files.indexOf(own);
		String root = files.substring(0, pos);
		
		ArrayList<String> apps = new ArrayList<String>();
		final ArrayList<String> pkgs = new ArrayList<String>();
		PackageManager pm = getPackageManager();
		
		File[] dirs = new File(root).listFiles();
		if (dirs != null) {
			for (File dir: dirs) {
				if (!dir.isDirectory()) continue;
				String pkg = dir.getName();
				try {
					ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
					String label = ai.loadLabel(pm).toString();
					pkgs.add(pkg);
					apps.add(label + "\n" + pkg);
				} catch (NameNotFoundException e) {
					// ignore
				}
			}
		}
		
		final String[] items = apps.toArray(new String[apps.size()]);
		new AlertDialog.Builder(this)
			.setTitle(R.string.select_app)
			.setItems(items, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String pkg = pkgs.get(which);
					
					File backup = new File(dir + "/" + pkg);
					File data = new File(files.replace(own, pkg)).getParentFile();
					
					final File source = restore ? backup : data;
					final File destination = restore ? data : backup;
					status.setText(source.getAbsolutePath() + "\n\n-->\n\n" + destination.getAbsolutePath());
					Log.i(TAG, (restore ? "Restore " : "Backup ") + source + " -> " + destination);
					new Thread(new Runnable() {
						@Override public void run() {
							String result;
							try {
								if (!source.exists() || !source.isDirectory()) {
									throw new IOException("Source folder is missing or inaccessible: " + source + "\nCheck whether Parallel Space allows access to this app's data.");
								}
								if (!destination.exists() && !destination.mkdirs() && !destination.isDirectory()) {
									throw new IOException("Cannot create destination folder: " + destination);
								}
								copyDirectoryContents(source, destination);
								result = "Copy completed successfully.\n\nSource: " + source + "\nDestination: " + destination;
							} catch (Exception e) {
								Log.e(TAG, "Copy failed", e);
								result = "COPY FAILED\n\n" + e.getMessage();
							}
							final String message = result;
							runOnUiThread(new Runnable() {
								@Override public void run() {
									status.setText(source.getAbsolutePath() + "\n\n-->\n\n" + destination.getAbsolutePath() + "\n\n" + message);
									String toastMessage = message.startsWith("COPY FAILED")
        ? "Copy failed"
        : getString(R.string.ended);

Toast.makeText(
        MainActivity.this,
        toastMessage,
        Toast.LENGTH_LONG
).show();
	}


	// Use Java file I/O instead of the device's external `cp` command. Some
	// Android builds reject the legacy source/. syntax used by the original app.
	private static void copyDirectoryContents(File source, File destination) throws IOException {
		File[] children = source.listFiles();
		if (children == null) throw new IOException("Cannot read source folder: " + source);
		if (!destination.exists() && !destination.mkdirs() && !destination.isDirectory()) {
			throw new IOException("Cannot create folder: " + destination);
		}
		for (File child : children) {
			File target = new File(destination, child.getName());
			if (child.isDirectory()) {
				copyDirectoryContents(child, target);
			} else if (child.isFile()) {
				File parent = target.getParentFile();
				if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory())
					throw new IOException("Cannot create folder: " + parent);
				try (FileInputStream in = new FileInputStream(child); FileOutputStream out = new FileOutputStream(target)) {
					byte[] buffer = new byte[8192];
					int count;
					while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
					out.getFD().sync();
				}
			}
		}
	}

}
