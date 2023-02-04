#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <maps/wikimap/mapspro/services/autocart/libs/utils/include/multithreading.h>

namespace maps::wiki::autocart::tests {

class NumberReader : public DataReader<int> {
public:
    NumberReader(const std::vector<int>& numbers)
        : numbers_(numbers) {}

private:
    void Read() override {
        for (const int& number : numbers_) {
            outQueue_->Enqueue(number);
        }
    }

    const std::vector<int>& numbers_;
};

class SquareProcessor : public DataProcessor<int, int> {
private:
    std::optional<int> Do(const int& number) override {
        return number * number;
    }

    SquareProcessor* Clone() const override {
        return new SquareProcessor(*this);
    }
};

class NumberWriter : public DataWriter<int> {
public:
    NumberWriter(std::vector<int>* numbers)
        : numbers_(numbers) {}
private:
    void Write(const int& number) override {
        numbers_->push_back(number);
    }

    std::vector<int>* numbers_;
};

Y_UNIT_TEST_SUITE(multithreading_tests)
{

    Y_UNIT_TEST(test_multithreading_square)
    {

        size_t processorsNum = 4;
        std::vector<int> numbers;
        for (size_t i = 0; i < 10; i++) {
            numbers.push_back(i);
        }

        std::vector<int> squares;

        MultithreadDataProcessor<int, int> mtp;
        mtp.SetReader(new NumberReader(numbers));
        mtp.SetProcessors(new SquareProcessor(), processorsNum);
        mtp.SetWriter(new NumberWriter(&squares));

        mtp.Run();

        EXPECT_TRUE(squares.size() == numbers.size());
        for (const auto& number : numbers) {
            EXPECT_TRUE(std::find(squares.begin(), squares.end(),
                                  number * number) != squares.end());
        }
    }

} //Y_UNIT_TEST_SUITE(multithreading_tests)

} //namespace maps::wiki::autocart::tests


