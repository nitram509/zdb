package io.zeebe;


public interface ZeebeStatus {

  /**
   * Returns an well formated string which contains all status information about Zeebe, it searchs
   * in the given partition state for the necessary information.
   *
   * <p>Returned information's are:
   *
   * <ul>
   *   <li>lastExportedPosition
   *   <li>lastProcessedPosition
   * </ul>
   *
   * @param partitionState the state of the partition
   * @return a well formated string which contains all information
   */
  public String status(PartitionState partitionState);
}
