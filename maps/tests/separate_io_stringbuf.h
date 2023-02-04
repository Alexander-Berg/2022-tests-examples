#pragma once

#include <streambuf>

namespace maps::apiteka {
class SeparateIOStringBuf final : public std::streambuf {
public:
    explicit SeparateIOStringBuf(std::string input)
    {
        output_.resize(256);
        setInput(std::move(input));
    }

    void setInput(std::string input)
    {
        input_ = std::move(input);
        sync();
    }

    std::string_view output() const noexcept
    {
        const auto begin{pbase()};
        return {begin, static_cast<std::size_t>(pptr() - begin)};
    }

    const std::string& outputRawContents() const noexcept
    {
        return output_;
    }

private:
    int sync() override
    {
        {
            const auto gbegin{input_.data()};
            setg(gbegin, gbegin, gbegin + input_.size());
        }

        {
            const auto pbegin{output_.data()};
            setp(pbegin, pbegin + output_.size());
        }
        return {};
    }

    int_type underflow() override { return traits_type::eof(); }

    int_type overflow(int_type ch = traits_type::eof()) override
    {
        if (!traits_type::eq_int_type(ch, traits_type::eof())) {
            try {
                output_.push_back(char_type{});
                output_.resize(output_.capacity());

                const auto begin{output_.data()};
                setp(begin, begin + output_.size());
            } catch (...) {
                return traits_type::eof();
            }

            return sputc(traits_type::to_char_type(ch));
        }

        return traits_type::not_eof(ch);
    }

    std::string input_;
    std::string output_;
};

} // namespace maps::apiteka
