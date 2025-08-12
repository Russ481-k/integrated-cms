import { getApiUrl } from "@/lib/config/api-config";

/**
 * Generates the public download URL for a given file ID.
 * @param fileId The ID of the file.
 * @returns The public URL string for downloading the file.
 */
export const getPublicFileDownloadUrl = (fileId: number): string => {
  if (typeof fileId !== "number" || fileId <= 0) {
    console.warn(
      "Invalid fileId provided to getPublicFileDownloadUrl:",
      fileId
    );
    return "#";
  }
  return getApiUrl.fileDownload(fileId); // 파일 다운로드 URL
};
