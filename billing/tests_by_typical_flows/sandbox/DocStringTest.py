def findDuplicates(nums):
    out = []
    j = 0
    i = 0
    buff = 0
    count = 0
    while count < nums.__len__():
        if nums[j + i] == j + 1:
            i = i + 1
            count = count + 1
            continue

        elif nums[i + j] == nums[nums[i + j]-1]:
            j = j + 1

        else:
            buff = nums[j + i]
            nums[j + i] = nums[nums[i + j]-1]
            nums[buff-1] = buff
        count = count + 1

    return nums[i:i + j + 1]

    """
    :type nums: List[int]
    :rtype: List[int]
    """

print findDuplicates([4,3,2,7,8,2,3,1])