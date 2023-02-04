package ru.auto.salesman.tasks.user.push

import ru.auto.salesman.model.AutoruUser
import ru.auto.salesman.tasks.Partition
import zio.{Managed, UManaged}

class DummyPartitionedUserSource(users: Map[Partition, Stream[AutoruUser]])
    extends PartitionedUserSource {

  def getSortedUsersWithActiveOffers(
      partition: Partition
  )(withIdMoreThan: Option[AutoruUser]): UManaged[Stream[AutoruUser]] =
    Managed.succeed(
      users(partition).filter(user => withIdMoreThan.forall(user.id > _.id))
    )

}
