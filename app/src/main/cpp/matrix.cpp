//
// Created by goann on 6/06/2024.
//


#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "matrix.h"

#include <jni.h>

///////////////////////////////////////////////////////////////////////////////
// Macros

/// Evaluates to non-zero if values a and b are within MATRIX_EPSILON of each
/// other (inclusive), zero if not

#define WITHIN_EPSILON(a,b) (fabs((a)-(b)) <= matrix_epsilon)


// Value of epsilon for matrix computations. Determinants whose absolute values
// are less than or equial to matrix_epsilon are considered zero and therefore
// the corresponding matrix is singular and non invertable. A multiplier for a
// elementary row operation must not be within epsilon of zero. Unit test results
// must be within matrix_epsilon (inclusive) to be considered successful.
double matrix_epsilon = MATRIX_EPSILON_DEFAULT;


extern "C" JNIEXPORT void JNICALL
Java_com_work_libtest_MainActivity_lMatrixCreate(JNIEnv* env, jobject , jint rows, jint cols) {
    matrix_create(rows,cols);
}

////////////////////////////////////////////////////////////////////////////////
// Creates a matrix of specified size. The number of rows and columns may be
// zero but elements of the matrix will not be accessable until its size is
// fixed. It may be fixed using matrix_set_size() or when it is the destination
// of an operation on one or more other matrices. Elements of the matrix will
// be initialised to 0.0.
//
// \brief Create a matrix.
// \param rows The number of rows for the new matrix. May be zero.
// \param cols The number of columns for the new matrix. May be zero.
// \return Pointer to new matrix or NULL on failure.

matrix_t *matrix_create(int rows, int cols)
{
    matrix_t *m;

    // Allocate matrix
    if ((m = (matrix_t *)malloc(sizeof(matrix_t))) == NULL)
        return NULL;

    m->rows = (rows > 0) ? rows : 0;
    m->cols = (cols > 0) ? cols : 0;
    m->element = NULL;

    // Allocate space for matrix elements
    if (m->rows && m->cols)
    {
        if ((m->element = (double *)malloc(sizeof(double) * m->rows * m->cols)) != NULL)
        {
            double *ptr;
            int    count;

            for (count = m->rows * m->cols, ptr = m->element; count > 0; count--, ptr++)
                *ptr = 0.0;
        }
        else
        {
            free(m);
            m = NULL;
        }
    }

    return m;
}



////////////////////////////////////////////////////////////////////////////////
// Destoys a matrix and all related resources.
//
// \brief Destroy a matrix.
// \param matrix Pointer to the matrix to be destroyed. If \p matrix is NULL,
// no action is taken.

void matrix_destroy(matrix_t *matrix)
{
    if (matrix)
    {
        if (matrix->element)
        {
            free(matrix->element);
            matrix->element = NULL;
        }
        matrix->rows = 0;
        matrix->cols = 0;
        free(matrix);
    }
}



////////////////////////////////////////////////////////////////////////////////
// Creates a duplicate of specified matrix with its own copy of elements.
//
// \brief Duplicate a matrix.
// \param matrix Pointer to the matrix to be duplicated.
// \return Pointer to new matrix that is a duplicate of the specified matrix.
// Will return NULL if \p matrix was NULL or a memory allocation failure occured.

matrix_t *matrix_duplicate(const matrix_t *matrix)
{
    matrix_t *m;

    if (!matrix)
        return NULL;

    // Allocate matrix
    if ((m = (matrix_t *)malloc(sizeof(matrix_t))) == NULL)
        return NULL;

    m->rows = (matrix->rows > 0) ? matrix->rows : 0;
    m->cols = (matrix->cols > 0) ? matrix->cols : 0;
    m->element = NULL;

    // Allocate space for matrix elements
    if (m->rows && m->cols)
    {
        if ((m->element = (double *)malloc(sizeof(double) * m->rows * m->cols)) != NULL)
            memcpy(m->element, matrix->element, sizeof(double) * m->rows * m->cols);
        else
            m->rows = m->cols = 0;
    }

    return m;
}



////////////////////////////////////////////////////////////////////////////////
// Copies an array of double values into the elements of specified matrix.
// assumes the array is in row major format.
//
// \brief Load matrix with array of double values.
// \param matrix Pointer to the matrix to be loaded.
// \param data Pointer to an array of doubles holding the elements to be loaded
// into \p matrix in row major format.
// \param n Number of doubles contained in \p data.
// \return Error code.
// \retval matrixOk No error. Elements laoded OK.
// \retval matrixErrBadArg \p matrix was NULL.
// \retval matrixErrBadIndex \p n was smaller than the number of elements that \p matrix holds.

matrix_err_t matrix_load(matrix_t *matrix, const double *data, size_t n)
{
    // Sanity check.
    if (!matrix)
        return matrixErrBadArg;

    // Special case. Nothing to do.
    if (!matrix->element || (matrix->rows < 1) || (matrix->cols < 1))
        return matrixOk;

    // Make sure we have enough data
    if (n < (matrix->rows * matrix->cols))
        return matrixErrBadIndex;

    memcpy(matrix->element, data, matrix->rows * matrix->cols * sizeof(double));
    return matrixOk;
}



////////////////////////////////////////////////////////////////////////////////
// Returns the value of a matrix element at specified row and column.
//
// \brief Get matrix element.
// \param matrix Pointer to the matrix.
// \param row The row of the required element indexed starting from zero.
// \param col The column of the required element indexed starting from zero.
// \return Value of the specified element. Will return 0.0 if \p matrix was
// NULL or the row/col was out of range.

double matrix_get_element(matrix_t *matrix, int row, int col)
{
    if (!matrix || !matrix->element || (row < 0) || (row >= matrix->rows) || (col < 0) || (col >= matrix->cols))
        return 0.0;

    return matrix->element[(row * matrix->cols) + col];
}



////////////////////////////////////////////////////////////////////////////////
// Modifies the value of a matrix element at specified row and column.
//
// \brief Set matrix element.
// \param matrix Pointer to the matrix.
// \param row The row of the element to be modified indexed starting from zero.
// \param col The column of the element to be modified indexed starting from zero.
// \return Error code.
// \retval matrixOk No error. Element was modified.
// \retval matrixErrBadArg \p matrix was NULL.
// \retval matrixErrBadIndex \p row or \p col was out of bounds.

matrix_err_t matrix_set_element(matrix_t *matrix, int row, int col, double value)
{
    if (!matrix || !matrix->element)
        return matrixErrBadArg;

    if ((row < 0) || (row >= matrix->rows) || (col < 0) || (col >= matrix->cols))
        return matrixErrBadIndex;

    matrix->element[(row * matrix->cols) + col] = value;
    return matrixOk;
}



////////////////////////////////////////////////////////////////////////////////
// Calculates the transpose of a matrix. x = a'
//
// \brief Transpose matrix.
// \param x Pointer to the result matrix that will be modified to hold the
// result. This may point to the same matrix as \p a.
// \param a Pointer to the matrix to be transposed.
// \return Error code.
// \retval matrixOk No error. Element was modified.
// \retval matrixErrBadArg \p a or \p r was NULL.
// \retval matrixErrMemAllocation A memory allocation error occured.

matrix_err_t matrix_transpose(matrix_t *x, const matrix_t *a)
{
    double *new_element;
    int     new_rows, new_cols, r, c;

    // Sanity check.
    if (!a || !x)
        return matrixErrBadArg;

    // Special case
    if (!a->element || (a->rows < 1) || (a->cols < 1))
    {
        if (x->element)
        {
            free(x->element);
            x->element = NULL;
        }
        x->rows = 0;
        x->cols = 0;
        return matrixOk;
    }

    new_rows = a->cols;
    new_cols = a->rows;

    // Allocate
    if ((new_element = (double *)malloc(sizeof(double) * new_rows * new_cols)) == NULL)
        return matrixErrMemAllocation;

    // Transpose into buffer
    for (r = 0; r < new_rows; r++)
        for (c = 0; c < new_cols; c++)
            new_element[(r * new_cols) + c] = a->element[(c * a->cols) + r];

    // Set this buffer
    if (x->element)
    {
        free(x->element);
        x->element = NULL;
    }
    x->rows = new_rows;
    x->cols = new_cols;
    x->element = new_element;

    return matrixOk;
}



////////////////////////////////////////////////////////////////////////////////
// Calculates the multiplication of one matrix with another. x = a x b
//
// \brief Multiplies two matrices.
// \param x Pointer to the result matrix that will be modified to hold the
// result of the multiplication. This may point to the same matrix as \p a or \p b.
// \param a Pointer to the matrix that is the left hand side of the multiplication.
// \param b Pointer to the matrix that is the right hand side of the multiplication.
// \return Error code.
// \retval matrixOk No error. Element was modified.
// \retval matrixErrBadArg \p a, \p b or \p x was NULL.
// \retval matrixErrBadIndex The number of columns of \p a does not match the number of rows
// of \p b.
// \retval matrixErrMemAllocation Memory allocation error.

matrix_err_t matrix_multiply(matrix_t *x, const matrix_t *a, const matrix_t *b)
{
    double *new_element;
    int     new_rows, new_cols, r, c, i;

    // Sanity check.
    if (!a || !b || !x || !a->element || !b->element || (a->rows < 1) || (a->cols < 1) || (b->rows < 1) || (b->cols < 1))
        return matrixErrBadArg;

    if (a->cols != b->rows)
        return matrixErrBadIndex;

    new_rows = a->rows;
    new_cols = b->cols;

    // Allocate
    if ((new_element = (double *)malloc(sizeof(double) * new_rows * new_cols)) == NULL)
        return matrixErrMemAllocation;

    // Multiply into buffer
    for (r = 0; r < new_rows; r++)
    {
        for (c = 0; c < new_cols; c++)
        {
            new_element[(r * new_cols) + c] = 0.0;
            for (i = 0; i < a->cols; i++)
                new_element[(r * new_cols) + c] += a->element[(r * a->cols) + i] * b->element[(i * b->cols) + c];
        }
    }

    // Set this buffer
    if (x->element)
    {
        free(x->element);
        x->element = NULL;
    }
    x->rows = new_rows;
    x->cols = new_cols;
    x->element = new_element;

    return matrixOk;
}



////////////////////////////////////////////////////////////////////////////////
// Calculates new matrix by removing a specified row and column from some other
// matrix element.
//
// \brief Matrix obtained by removing row and column of argument.
// \param x Pointer to the result matrix. This may point to the same matrix as \p a.
// \param a Pointer to the matrix whose row and column is to be removed for the
// result.
// \return Error code.
// \retval matrixOk No error.
// \retval matrixErrBadArg \p a or \p x was NULL.
// \retval matrixErrBadIndex \p row or \p col was out of bounds.
// \retval matrixErrMemAllocation Memory allocation error.

matrix_err_t matrix_remove_row_col(matrix_t *x, const matrix_t *a, int row, int col)
{
    double *new_element, *src, *dst;
    int     new_rows, new_cols, r, c;

    // Sanity check.
    if (!a || !x || !a->element || (a->rows < 1) || (a->cols < 1))
        return matrixErrBadArg;

    if ((row < 0) || (row >= a->rows) || (col < 0) || (row >= a->cols))
        return matrixErrBadIndex;

    // Special case
    if ((a->rows == 1) || (a->cols == 1))
    {
        if (x->element)
        {
            free(x->element);
            x->element = NULL;
        }
        x->rows = 0;
        x->cols = 0;
        return matrixOk;
    }

    new_rows = a->rows - 1;
    new_cols = a->cols - 1;

    // Allocate
    if ((new_element = (double *)malloc(sizeof(double) * new_rows * new_cols)) == NULL)
        return matrixErrMemAllocation;

    // Remove the rows and columns
    src = a->element;
    dst = new_element;
    for (r = 0; r < a->rows; r++)
    {
        for (c = 0; c < a->cols; c++)
        {
            if ((r != row) && (c != col))
                *dst++ = *src++;
            else
                src++;
        }
    }

    // Set this buffer
    if (x->element)
    {
        free(x->element);
        x->element = NULL;
    }
    x->rows = new_rows;
    x->cols = new_cols;
    x->element = new_element;

    return matrixOk;
}



////////////////////////////////////////////////////////////////////////////////
// Calculates the cofactor (often called the adjunct) of an element of a matrix.
// It is the determinant of the matrix obtained by removing the row and column
// corresponding to that element multiplied by (-1)^(row+col).
//
// \brief Cofactor (adjunct) of a matrix element.
// \param m Pointer to the matrix whose cofactor is to be calculated.
// \param row Row of element in \p m whose cofactor is to be calculated.
// \param col Column of element in \p m whose cofactor is to be calculated.
// \param err Optional pointer to error code. Will be set to matrixOk if no
// error. Set to matrixErrBadArg if \p m is not a valid matrix,
// matrixErrNotSquare if \p m is not square, matrixErrBadIndex if \p row or
// \p col is out of bounds, matrixErrMemAllocation on memory allocation
// error. \p err may be NULL.
// \return Cofactor of element at \p row, \p col of matrix \p m. 0 on error.

double matrix_cofactor(const matrix_t *m, int row, int col, matrix_err_t *err)
{
    matrix_t	*n;
    double		 det, sign;
    matrix_err_t my_err;

    if ((n = matrix_create(0, 0)) == NULL)
    {
        if (err) *err = matrixErrMemAllocation;
        return 0.0;
    }

    if ((my_err = matrix_remove_row_col(n, m, row, col)) != matrixOk)
    {
        if (err) *err = my_err;
        matrix_destroy(n);
        return 0.0;
    }

    det  = matrix_determinant(n, &my_err);
    sign = ((row + col) & 1) ? -1.0 : 1.0;
    matrix_destroy(n);

    if (err) *err = my_err;
    return det * sign;
}



////////////////////////////////////////////////////////////////////////////////
// Calculates the determinant of a matrix.
//
// \brief Determinant of a matrix.
// \param m Pointer to the matrix whose determinant is to be calculated.
// \param err Optional pointer to error code. Will be set to matrixOk if no
// error. Set to matrixErrBadArg if \p m is not a valid matrix,
// matrixErrNotSquare if \p m is not square, matrixErrBadIndex if \p row or
// \p col is out of bounds, matrixErrMemAllocation on memory allocation
// error. \p err may be NULL.
/// \return Determinant matrix \p m. 0 on error.

double matrix_determinant(const matrix_t *m, matrix_err_t *err)
{
    matrix_err_t my_err;
    double det;
    int i;

    if (!m || !m->element || (m->rows < 1) || (m->cols < 1))
    {
        if (err) *err = matrixErrBadArg;
        return 0.0;
    }

    if (m->rows != m->cols)
    {
        if (err) *err = matrixErrNotSquare;
        return 0.0;
    }

    if (m->rows == 1)
    {
        if (err) *err = matrixOk;
        return m->element[0];
    }

    if (m->rows == 2)
    {
        if (err) *err = matrixOk;
        return m->element[0] * m->element[3] - m->element[1] * m->element[2];
    }

    if (m->rows == 3)
    {
        if (err) *err = matrixOk;
        return	  (m->element[0] * (m->element[4] * m->element[8] - m->element[5] * m->element[7]))
                    - (m->element[1] * (m->element[3] * m->element[8] - m->element[5] * m->element[6]))
                    + (m->element[2] * (m->element[3] * m->element[7] - m->element[4] * m->element[6]));
    }

    // For determinants of matrices greater then 3x3, use expansion by minors.
    det = 0;
    for (i = 0; i < m->cols; i++)
    {
        det += m->element[i] * matrix_cofactor(m, 0, i, &my_err);
        if (my_err != matrixOk)
            break;
    }

    if (err) *err = my_err;
    return det;
}



////////////////////////////////////////////////////////////////////////////////
// Calculates the inversion of a matrix such that the matrix multiplied by its
// inversion is equal to the identity matrix. The matrix must be square.
// x = a^-1
//
// \brief Invert a square matrix.
// \param x Pointer to the result matrix. May be same as \p a.
// \param a Pointer to the matrix that is to be inverted.
// \return Error code
// \retval matrixOk No error occured. Matrix was successfully inverted.
// \retval matrixErrBadArg The \p x or \p a is NULL or \p a has no rows or
// columns.
// \retval matrixErrNotSquare The matrix \p a is not square.
// \retval matrixErrSingular The matrix \p a is singular and therefore has no
// inverse.

matrix_err_t matrix_invert(matrix_t *x, const matrix_t *a)
{
    matrix_err_t err;
    double  det, cofactor;
    double *new_element;
    int     new_rows, new_cols, r, c;

    if (!x)
        return matrixErrBadArg;

    // This uses an analytic solution based on Cramer's rule. It is very simple
    // but can get inefficient for larger matrices due to its recursive nature.
    // The inverse is calculated as the inverse of the determinant multiplied by
    // the adjugate matrix (transpose of cofactor matrix).
    // FIXME: More efficient method for larger matrices. Using Gauss-Jordan method
    //        http://www.mathsisfun.com/algebra/matrix-inverse-row-operations-gauss-jordan.html
    //        http://www.cliffsnotes.com/study_guide/Using-Elementary-Row-Operations-to-Determine-A1.topicArticleId-20807,articleId-20784.html

    det = matrix_determinant(a, &err);
    if (err != matrixOk)
        return err;

    if (WITHIN_EPSILON(det, 0.0))
        return matrixErrSingular;

    // If we get this far, we know a is square and at least 1x1 and det(a) > epsilon.
    // Allocate space for the elements of the inverse.
    new_rows = a->rows;
    new_cols = a->cols;

    // Allocate
    if ((new_element = (double *)malloc(sizeof(double) * new_rows * new_cols)) == NULL)
        return matrixErrMemAllocation;

    // Calculate the inverse as transpose of cofactor matrix with each element divided by determinant.
    for (r = 0; r < new_rows; r++)
    {
        for (c = 0; c < new_cols; c++)
        {
            cofactor = matrix_cofactor(a, c, r, &err);
            if (err != matrixOk)
            {
                free(new_element);
                return err;
            }

            new_element[(r * new_cols) + c] = cofactor / det;
        }
    }

    // Save the result
    if (x->element)
    {
        free(x->element);
        x->element = NULL;
    }
    x->rows = new_rows;
    x->cols = new_cols;
    x->element = new_element;

    return matrixOk;

}



////////////////////////////////////////////////////////////////////////////////
// Swaps two rows of a matrix. row1 <--> row2
//
// \brief Apply "swap" elementary row operation.
// \param m Pointer to the matrix to which the ERO is to be apllied.
// \param row1 Index of first row to swap (0 based).
// \param row2 Index of second row to swap (0 based).
// \return Error code
// \retval matrixOk No error occured. Rows were successfully swapped.
// \retval matrixErrBadArg The matrix \p m is NULL or has no elements.
// \retval matrixErrBadIndex \p row1 or \p row2 is out of bounds.

matrix_err_t matrix_ero_swap(matrix_t *m, int row1, int row2)
{
    double *ptr1, *ptr2, temp;
    int    c;

    if (!m || !m->element || (m->rows < 1) || (m->cols < 1))
        return matrixErrBadArg;

    if ((row1 < 0) || (row1 >= m->rows) || (row2 < 0) || (row2 >= m->rows))
        return matrixErrBadIndex;

    ptr1 = &m->element[row1 * m->cols];
    ptr2 = &m->element[row2 * m->cols];
    for (c = m->cols; c > 0 ; c--, ptr1++, ptr2++)
    {
        temp  = *ptr1;
        *ptr1 = *ptr2;
        *ptr2 = temp;
    }

    return matrixOk;
}



////////////////////////////////////////////////////////////////////////////////
// Multiply each element of a matrix row by specified non-zero multiplier.
// row * multiplier ---> row
//
// \brief Apply "multiply" elementary row operation.
// \param m Pointer to the matrix to which the ERO is to be apllied.
// \param row Index of row to be multiplied (0 based).
// \param multiplier Value that the specified row is multiplied by. \p multiplier
// may not be zero.
// \return Error code
// \retval matrixOk No error occured. Row was successfully multiplied.
// \retval matrixErrBadArg The matrix \p m is NULL or has no elements.
// \retval matrixErrBadIndex \p row is out of bounds.
// \retval matrixErrZeroNotAllowed \p multiplier was zero. The multiplier is
// deemed to be zero if it is within matrix_epsilon of zero.

matrix_err_t matrix_ero_multiply(matrix_t *m, int row, double multiplier)
{
    double *ptr;
    int    c;

    if (!m || !m->element || (m->rows < 1) || (m->cols < 1))
        return matrixErrBadArg;

    if (WITHIN_EPSILON(multiplier, 0.0))
        return matrixErrZeroNotAllowed;

    if ((row < 0) || (row >= m->rows))
        return matrixErrBadIndex;

    ptr = &m->element[row * m->cols];
    for (c = m->cols; c > 0 ; c--, ptr++)
        *ptr *= multiplier;

    return matrixOk;
}



////////////////////////////////////////////////////////////////////////////////
// Add a non-zero multiple of one row to another.
// row1 + row2 * multiplier ---> row1
//
// \brief Apply "add multiple" elementary row operation.
// \param m Pointer to the matrix to which the ERO is to be apllied.
// \param row1 Index of target row for operation (0 based).
// \param row2 Index of row to be added to \p row1 (0 based).
// \param multiplier Multiple of \p row2 that is added to \p row1. \p multiplier
// may not be zero.
// \return Error code
// \retval matrixOk No error occured. Row was successfully multiplied.
// \retval matrixErrBadArg The matrix \p m is NULL or has no elements.
// \retval matrixErrBadIndex \p row1 or \p row2 is out of bounds.
// \retval matrixErrZeroNotAllowed \p multiplier was zero. The multiplier is
// deemed to be zero if it is within matrix_epsilon of zero.

matrix_err_t matrix_ero_add_multiple(matrix_t *m, int row1, int row2, double multiplier)
{
    double *ptr1, *ptr2;
    int    c;

    if (!m || !m->element || (m->rows < 1) || (m->cols < 1))
        return matrixErrBadArg;

    if (WITHIN_EPSILON(multiplier, 0.0))
        return matrixErrZeroNotAllowed;

    if ((row1 < 0) || (row1 >= m->rows) || (row2 < 0) || (row2 >= m->rows))
        return matrixErrBadIndex;

    ptr1 = &m->element[row1 * m->cols];
    ptr2 = &m->element[row2 * m->cols];
    for (c = m->cols; c > 0 ; c--, ptr1++, ptr2++)
        *ptr1 += *ptr2 * multiplier;

    return matrixOk;
}



#if defined(MATRIX_UNIT_TEST)
#include <stdio.h>
#define PRINT_ERR(s)	do { errs++; printf(s); } while(0)

////////////////////////////////////////////////////////////////////////////////
// Run matrix unit tests. Details of any failures will be sent to stdout.
//
// \brief Test matrix functions.
// \return Error count. Number of errors detected.
// \retval 0 No error. All tests passed.

int matrix_test(void)
{
	int			errs = 0;
	int			err, r, c;
	double		det;
	matrix_t	*result, *mat2x2, *mat3x3, *mat4x4, *mat2x3, *mat3x2;
	matrix_err_t my_err;
	static const double zero6[6]    = { 0.0, 0.0, 0.0,  0.0,  0.0,  0.0 };
	static const double data1[6]    = { 1.0, 2.0, 3.0,  4.0,  5.0,  6.0 };
	static const double data2[6]    = { 7.0, 8.0, 9.0, 10.0, 11.0, 12.0 };
	static const double data2x2[4]  = { 4.0, 6.0, 3.0, 8.0};
	static const double data3x3[9]  = { 6.0, 1.0, 1.0, 4.0, -2.0, 5.0, 2.0, 8.0, 7.0 };
	static const double data4x4[16] = { 3.0, 2.0, -1.0, 4.0, 2.0, 1.0, 5.0, 7.0, 0.0, 5.0, 2.0, -6.0, -1.0, 2.0, 1.0, 0.0 };
	static const double data4x4nonsingular[16] = { 5,3,7,1,2,4,9,3,3,6,4,8,2,9,6,5 };
	static const double data4x4singular[16]	   = { 1,2,3,4,2,4,6,8,3,6,4,8,2,9,6,5 };

	result = matrix_create(0, 0);
	mat2x2 = matrix_create(2, 2);
	mat3x3 = matrix_create(3, 3);
	mat4x4 = matrix_create(4, 4);
	mat2x3 = matrix_create(2, 3);
	mat3x2 = matrix_create(3, 2);

	if (!result || !mat2x2 || !mat3x3 || !mat4x4 || !mat2x3)
	{
		PRINT_ERR("MATRIX TEST FAILED: Unable to allocate memory.\n");
		if (result) matrix_destroy(result);
		if (mat2x2) matrix_destroy(mat2x2);
		if (mat3x3) matrix_destroy(mat3x3);
		if (mat4x4) matrix_destroy(mat4x4);
		if (mat2x3) matrix_destroy(mat2x3);
		if (mat3x2) matrix_destroy(mat3x2);
		return errs;
	}

	// Test element read: Poke row major elements and read out individually
	matrix_load(mat2x3, data1, 6);
	if ((matrix_get_element(mat2x3, 0, 0) != 1.0) || (matrix_get_element(mat2x3, 0, 1) != 2.0) || (matrix_get_element(mat2x3, 0, 2) != 3.0) ||
		(matrix_get_element(mat2x3, 1, 0) != 4.0) || (matrix_get_element(mat2x3, 1, 1) != 5.0) || (matrix_get_element(mat2x3, 1, 2) != 6.0))
		PRINT_ERR("MATRIX TEST FAILED: matrix_get_element() element mismatch.\n");

	// Test element write: Write elements individually then test againse memory containing row major version.
	err = 0;
	matrix_load(mat2x3, zero6, 6);
	if (matrix_set_element(mat2x3, 0, 0, 1.0) != 0) err = 1;
	if (matrix_set_element(mat2x3, 0, 1, 2.0) != 0) err = 1;
	if (matrix_set_element(mat2x3, 0, 2, 3.0) != 0) err = 1;
	if (matrix_set_element(mat2x3, 1, 0, 4.0) != 0) err = 1;
	if (matrix_set_element(mat2x3, 1, 1, 5.0) != 0) err = 1;
	if (matrix_set_element(mat2x3, 1, 2, 6.0) != 0) err = 1;
	if (!err)
	{
		if (memcmp(mat2x3->element, data1, mat2x3->rows * mat2x3->cols * sizeof(double)) != 0)
			PRINT_ERR("MATRIX TEST FAILED: matrix_set_element() element mismatch.\n");
	}
	else
		PRINT_ERR("MATRIX TEST FAILED: matrix_set_element() element out of bounds.\n");

	// Test transpose:
	matrix_load(mat2x3, data1, 6);
	matrix_transpose(result, mat2x3);
	if ((result->rows == 3) && (result->cols == 2))
	{
		if ((matrix_get_element(result, 0, 0) != 1.0) || (matrix_get_element(result, 0, 1) != 4.0) ||
			(matrix_get_element(result, 1, 0) != 2.0) || (matrix_get_element(result, 1, 1) != 5.0) ||
			(matrix_get_element(result, 2, 0) != 3.0) || (matrix_get_element(result, 2, 1) != 6.0))
			PRINT_ERR("MATRIX TEST FAILED: matrix_transpose() element mismatch.\n");
	}
	else
	{
		errs++;
		printf("MATRIX TEST FAILED: matrix_transpose() transposes a 2x3 matrix to %dx%d.\n", result->rows, result->cols);
	}

	// Test multiply:
	matrix_load(mat2x3, data1, 6);
	matrix_load(mat3x2, data2, 6);
	matrix_multiply(result, mat2x3, mat3x2);
	if ((result->rows == 2) && (result->cols == 2))
	{
		if ((matrix_get_element(result, 0, 0) !=  58.0) || (matrix_get_element(result, 0, 1) !=  64.0) ||
			(matrix_get_element(result, 1, 0) != 139.0) || (matrix_get_element(result, 1, 1) != 154.0))
			PRINT_ERR("MATRIX TEST FAILED: matrix_multiply() element mismatch.\n");
	}
	else
	{
		errs++;
		printf("MATRIX TEST FAILED: matrix_multiply() 2x3 multiplied by 3x2 matrix results in a %dx%d matrix.\n", result->rows, result->cols);
	}

	// Determinant 2x2
	matrix_load(mat2x2, data2x2, 4);
	det = matrix_determinant(mat2x2, &my_err);
	if (my_err == matrixOk)
	{
		if (!WITHIN_EPSILON(det, 14.0))
		{
			errs++;
			printf("MATRIX TEST FAILED: matrix_determinant() bad 2x2 determinant. Calculated %g, expected 14.\n", det);
		}
	}
	else
		PRINT_ERR("MATRIX TEST FAILED: matrix_determinant() unable to calculate 2x2 determinant.\n");

	// Determinant 3x3
	matrix_load(mat3x3, data3x3, 9);
	det = matrix_determinant(mat3x3, &my_err);
	if (my_err == matrixOk)
	{
		if (!WITHIN_EPSILON(det, -306.0))
		{
			errs++;
			printf("MATRIX TEST FAILED: matrix_determinant() bad 3x3 determinant. Calculated %g, expected -306.\n", det);
		}
	}
	else
		PRINT_ERR("MATRIX TEST FAILED: matrix_determinant() unable to calculate 3x3 determinant.\n");

	// Determinant 4x4
	matrix_load(mat4x4, data4x4, 16);
	det = matrix_determinant(mat4x4, &my_err);
	if (my_err == matrixOk)
	{
		if (!WITHIN_EPSILON(det, -418.0))
		{
			errs++;
			printf("MATRIX TEST FAILED: matrix_determinant() bad 4x4 determinant. Calculated %g, expected -418.\n", det);
		}
	}
	else
		PRINT_ERR("MATRIX TEST FAILED: matrix_determinant() unable to calculate 4x4 determinant.\n");

	// Inversion of a singular matrix
	matrix_load(mat4x4, data4x4singular, 16);
	my_err = matrix_invert(result, mat4x4);
	if (my_err == matrixOk)
		PRINT_ERR("MATRIX TEST FAILED: matrix_invert() managed to invert a singular matrix.\n");
	else if (my_err != matrixErrSingular)
		PRINT_ERR("MATRIX TEST FAILED: matrix_invert() failed to identify a singular matrix.\n");

	// Inversion of a non-singular matrix
	matrix_load(mat4x4, data4x4nonsingular, 16);
	my_err = matrix_invert(result, mat4x4);
	if (my_err == matrixOk)
	{
		// Multiplying the inverse by the original should yield the identity matrix
		if (matrix_multiply(result, result, mat4x4) == matrixOk)
		{
			err = 0;
			for (r = 0; r < result->rows; r++)
			{
				for (c = 0; c < result->cols; c++)
				{
					if (!WITHIN_EPSILON(matrix_get_element(result, r, c), ((r == c) ? 1.0 : 0.0)))
					{
//						printf("Identity[%d][%d] = %g expecting %g\n", r, c, matrix_get_element(result, r, c), ((r == c) ? 1.0 : 0.0));
						err++;
					}
				}
			}

			if (err > 0)
				PRINT_ERR("MATRIX TEST FAILED: matrix_invert() did not generate an inverse matrix.\n");
		}
		else
			PRINT_ERR("MATRIX TEST FAILED: matrix_invert() unable to multiply inverse with self.\n");
	}
	else if (my_err == matrixErrSingular)
		PRINT_ERR("MATRIX TEST FAILED: matrix_invert() thought a non-sigular matrix was non-invertable.\n");
	else
		PRINT_ERR("MATRIX TEST FAILED: matrix_invert() failed.\n");

	// Elementary row operations: swap
	matrix_load(mat2x2, data2x2, 4);
	if (matrix_ero_swap(mat2x2, 0, 1) == matrixOk)
	{
		if ((matrix_get_element(mat2x2, 0, 0) != 3.0) || (matrix_get_element(mat2x2, 0, 1) != 8.0) ||
			(matrix_get_element(mat2x2, 1, 0) != 4.0) || (matrix_get_element(mat2x2, 1, 1) != 6.0))
			PRINT_ERR("MATRIX TEST FAILED: matrix_ero_swap() did not swap rows correctly.\n");
	}
	else
		PRINT_ERR("MATRIX TEST FAILED: matrix_ero_swap() failed.\n");

	// Elementary row operations: multiply
	matrix_load(mat2x2, data2x2, 4);
	if (matrix_ero_multiply(mat2x2, 1, 2.0) == matrixOk)
	{
		if ((matrix_get_element(mat2x2, 0, 0) != 4.0) || (matrix_get_element(mat2x2, 0, 1) != 6.0) ||
			(matrix_get_element(mat2x2, 1, 0) != 6.0) || (matrix_get_element(mat2x2, 1, 1) != 16.0))
			PRINT_ERR("MATRIX TEST FAILED: matrix_ero_multiply() did not multiply row correctly.\n");
	}
	else
		PRINT_ERR("MATRIX TEST FAILED: matrix_ero_multiply() failed.\n");

	// Elementary row operations: add multiple
	matrix_load(mat2x2, data2x2, 4);
	if (matrix_ero_add_multiple(mat2x2, 0, 1, 2.0) == matrixOk)
	{
		if ((matrix_get_element(mat2x2, 0, 0) != 10.0) || (matrix_get_element(mat2x2, 0, 1) != 22.0) ||
			(matrix_get_element(mat2x2, 1, 0) != 3.0)  || (matrix_get_element(mat2x2, 1, 1) != 8.0))
			PRINT_ERR("MATRIX TEST FAILED: matrix_ero_add_multiple() did not add multiple of row correctly.\n");
	}
	else
		PRINT_ERR("MATRIX TEST FAILED: matrix_ero_add_multiple() failed.\n");


	if (errs == 0) printf("MATRIX TEST: No errors detected.\n");
	if (result) matrix_destroy(result);
	if (mat2x2) matrix_destroy(mat2x2);
	if (mat3x3) matrix_destroy(mat3x3);
	if (mat4x4) matrix_destroy(mat4x4);
	if (mat2x3) matrix_destroy(mat2x3);
	if (mat3x2) matrix_destroy(mat3x2);
	return errs;
}

#endif