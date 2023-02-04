def cancel_order(pb, start_state):
    pb.order.status.status = 'IN_PROGRESS'
    pb.order.progress.state.id = start_state
    pb.order.cancelled.value = True
    pb.order.cancelled.comment = 'cancelled!'
    pb.order.cancelled.author = 'robot'
