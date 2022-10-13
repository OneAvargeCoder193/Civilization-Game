#define SETCOUNTRY(x, y, cont) if (x > 0 && x < width - 1 && y > 0 && y < height - 1 && input[y * width + x].type != 0) output[y * width + x].country = cont;

typedef struct
{
    float4 color;
    uint2 capital;
    bool exists;
} Country;

typedef struct
{
    int x, y;
    float rain;
    float temp;
    float height;
    int seed;
    Country country;
    long population;

    int type;
} Tile;

uint rand(ulong seed)
{
    uint res = (seed * 0x5DEECE66DL + (ulong)(sin((float)(seed & 0xff)) * seed)) & ((1L << 48) - 1);
    
    return res;
}

__kernel void update(__global Tile* input, __global Tile* output, int width, int height, ulong rand_offset)
{
    int i = get_global_id(0);
    Tile t = input[i];

    int x = i % width;
    int y = i / height;

    if (t.country.exists)
    {
        uint dir = rand(i + rand_offset) % 5;
        Country cont = input[y * width + x].country;
        // if (dir == 0)
        // {
        SETCOUNTRY(x - 1, y, cont);
    }
    
    output[i] = t;
}