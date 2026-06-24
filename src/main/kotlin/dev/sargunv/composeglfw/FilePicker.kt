package dev.sargunv.composeglfw

import java.nio.file.Path

/** Native file and folder picker for a Compose GLFW window. */
public interface FilePicker {
  /**
   * Opens a native dialog to choose one file.
   *
   * @return the selected file, or null if the dialog is canceled.
   */
  public fun openFile(
    defaultDirectory: Path? = null,
    filters: List<FileDialogFilter> = emptyList(),
  ): Path?

  /**
   * Opens a native dialog to choose one or more files.
   *
   * @return selected files, or an empty list if the dialog is canceled.
   */
  public fun openFiles(
    defaultDirectory: Path? = null,
    filters: List<FileDialogFilter> = emptyList(),
  ): List<Path>

  /**
   * Opens a native dialog to choose a save path.
   *
   * @return the selected save path, or null if the dialog is canceled.
   */
  public fun saveFile(
    defaultDirectory: Path? = null,
    defaultName: String? = null,
    filters: List<FileDialogFilter> = emptyList(),
  ): Path?

  /**
   * Opens a native dialog to choose one folder.
   *
   * @return the selected folder, or null if the dialog is canceled.
   */
  public fun pickFolder(defaultDirectory: Path? = null): Path?

  /**
   * Opens a native dialog to choose one or more folders.
   *
   * @return selected folders, or an empty list if the dialog is canceled.
   */
  public fun pickFolders(defaultDirectory: Path? = null): List<Path>
}

/**
 * File type filter shown by native file pickers.
 *
 * Extensions may be provided with or without a leading dot.
 */
public data class FileDialogFilter(
  /** Human-readable filter name, such as `Images`. */
  public val name: String,

  /** File extensions accepted by this filter, such as `png` and `jpg`. */
  public val extensions: List<String>,
) {
  init {
    require(name.isNotBlank()) { "File dialog filter name must not be blank" }
    require(extensions.isNotEmpty()) { "File dialog filter extensions must not be empty" }
    extensions.forEach { extension ->
      require(extension.trim().trimStart('.').isNotBlank()) {
        "File dialog filter extensions must not be blank"
      }
    }
  }
}
