# CKYC 4.0 Automation Framework
# Author : Aravindhan
# Created and developed by Aravindhan
@SearchAndDownload @Download @Deferred
Feature: Search And Download - Download
  As a CKYC operations user
  I want to download a CKYC record using identifiers captured by Search
  So that the downloaded response can be validated in a separate workflow

  # Automation is intentionally deferred until the Download UI flow is reviewed.
  # Future steps must consume:
  # Current Data/Search/latest/search-result.properties
  # Keys: maskedCkycNumber, ckycReferenceId, ckycReferenceIdentifier,
  # searchedPan, searchedMobile, capturedAt.
