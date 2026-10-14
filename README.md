# VirtualBackup — original UI with copy fix

This Android Studio project is based on the original uploaded VirtualBackup source. It keeps the original Backup/Restore selection flow and layout, while replacing the shell `cp -R -f` call with recursive Java file copying. This avoids the `cp: dir at '.../'` error shown on some Android builds and reports a real failure instead of always claiming the operation ended.

## Build with GitHub Actions (phone-friendly)
1. Upload the contents of this ZIP to your GitHub repository (include the hidden `.github` folder).
2. Open **Actions** and run **Build VirtualBackup APK** (or wait for the push-triggered run).
3. Download the `VirtualBackup-Android16-debug` artifact from the successful run and extract the APK.

## Important access limitation
This fix does not bypass Android/Parallel Space permissions. It can only copy data if the running VirtualBackup process can read the selected source directory and write the destination. If it reports `Permission denied`, `Cannot read source folder`, or `Source folder is missing or inaccessible`, Parallel Space is not exposing that private data to this app process. Test with nonessential data first; restore can overwrite app data.
