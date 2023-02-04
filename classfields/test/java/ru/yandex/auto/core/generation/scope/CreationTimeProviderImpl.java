package ru.yandex.auto.core.generation.scope;

import org.springframework.beans.factory.annotation.Required;

/** User: yan1984 Date: 04.05.2011 16:24:35 */
public class CreationTimeProviderImpl implements CreationTimeProvider {

  private long creationTime;

  @Required
  public void setCreationTime(long creationTime) {
    this.creationTime = creationTime;
  }

  @Override
  public long getCreationTime() {
    return creationTime;
  }
}
